/** ========================================================================= *
 * Copyright (C)  2017, 2018 Salesforce Inc ( http://www.salesforce.com/      *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Stephan H. Wissel (stw) <swissel@salesforce.com>              *
 *                                       @notessensei                         *
 * @version     1.0                                                           *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== *
 */
package net.wissel.salesforce.vertx.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.wissel.salesforce.vertx.ChaosMonkeyCometDServer;
import net.wissel.salesforce.vertx.Constants;
import net.wissel.salesforce.vertx.TestUtilities;
import net.wissel.salesforce.vertx.auth.Basic;
import net.wissel.salesforce.vertx.config.AuthConfig;
import net.wissel.salesforce.vertx.config.DedupConfig;
import net.wissel.salesforce.vertx.config.ListenerConfig;
import net.wissel.salesforce.vertx.dedup.MemoryDedup;

/**
 * Full handshake, advice, subscribe, connect flow against the mock
 * Salesforce CometD server
 */
@ExtendWith(VertxExtension.class)
public class CometDFlowTest {

	private static final String LISTEN_SUBJECT = "/event/Test__e";

	private Future<ChaosMonkeyCometDServer> deployPipeline(final Vertx vertx, final ListenerConfig listenerConfig) {
		TestUtilities.registerAuthCodec(vertx);

		final AuthConfig authConfig = new AuthConfig();
		authConfig.setAuthName("basic");
		// Basic auth hands the serverURL to the listener as server name
		authConfig.setServerURL("localhost");
		authConfig.setSfdcUser("User");
		authConfig.setSfdcPassword("pwd");

		final ChaosMonkeyCometDServer mock = new ChaosMonkeyCometDServer(vertx);
		return mock.start()
				.compose(server -> {
					listenerConfig.addParameter("port", String.valueOf(mock.actualPort()));
					listenerConfig.addParameter("ssl", "false");
					return vertx.deployVerticle(new Basic(),
							new DeploymentOptions().setConfig(JsonObject.mapFrom(authConfig)));
				})
				.compose(id -> vertx.deployVerticle(new CometD(),
						new DeploymentOptions().setConfig(JsonObject.mapFrom(listenerConfig))))
				.map(mock);
	}

	private ListenerConfig listenerConfig(final String instanceName, final String destination) {
		final ListenerConfig config = new ListenerConfig();
		config.setAuthName("basic");
		config.setListenSubject(LISTEN_SUBJECT);
		config.setInstanceName(instanceName);
		config.setEventBusAddresses(Set.of(destination));
		return config;
	}

	@Test
	public void stopListeningCompletesAfterStartupFailure(final Vertx vertx, final VertxTestContext testContext) {
		TestUtilities.registerAuthCodec(vertx);
		final ListenerConfig config = this.listenerConfig("cometdFail", "test:cometd-never");
		// Nothing answers this auth address, so the startup chain dies at its
		// very first step and no long poll is ever started
		config.setAuthName("doesNotExist");

		final CometD listener = new CometD();
		vertx.deployVerticle(listener, new DeploymentOptions().setConfig(JsonObject.mapFrom(config)))
				.compose(id -> {
					TestUtilities.startAllListeners(vertx);
					// Let the failing auth request settle
					return vertx.timer(300);
				})
				// This future stayed pending forever before the fix
				.compose(v -> listener.stopListening())
				.onComplete(testContext.succeeding(stopped -> testContext.completeNow()));
	}

	@Test
	public void eventTravelsToConfiguredAddress(final Vertx vertx, final VertxTestContext testContext) {
		final JsonObject payload = new JsonObject().put("Message__c", "hello").put("ObjectType__c", "Case");

		vertx.eventBus().<JsonObject>consumer("test:cometd", message -> testContext.verify(() -> {
			assertEquals(payload, message.body());
			assertEquals("test:cometd", message.headers().get(Constants.BUS_FINAL_DESTINATION));
			testContext.completeNow();
		}));

		this.deployPipeline(vertx, this.listenerConfig("cometd1", "test:cometd"))
				.onComplete(testContext.succeeding(mock -> {
					mock.enqueueEvents(ChaosMonkeyCometDServer.event(LISTEN_SUBJECT, payload, 1));
					TestUtilities.startAllListeners(vertx);
				}));
	}

	@Test
	public void eventPassesThroughDedupService(final Vertx vertx, final VertxTestContext testContext) {
		final JsonObject payload = new JsonObject().put("Message__c", "dedup me");

		vertx.eventBus().<JsonObject>consumer("test:cometd2", message -> testContext.verify(() -> {
			assertEquals(payload, message.body());
			testContext.completeNow();
		}));

		final ListenerConfig listenerConfig = this.listenerConfig("cometd2", "test:cometd2");
		listenerConfig.setUseDedupService(true);
		listenerConfig.setEventBusDedupAddress(Constants.BUS_DEDUP_PREFIX + "cmem");

		final DedupConfig dedupConfig = new DedupConfig();
		dedupConfig.setInstanceName("cmem");

		vertx.deployVerticle(new MemoryDedup(), new DeploymentOptions().setConfig(JsonObject.mapFrom(dedupConfig)))
				.compose(id -> this.deployPipeline(vertx, listenerConfig))
				.onComplete(testContext.succeeding(mock -> {
					mock.enqueueEvents(ChaosMonkeyCometDServer.event(LISTEN_SUBJECT, payload, 2));
					TestUtilities.startAllListeners(vertx);
				}));
	}

	@Test
	public void stopMessageDrainsAndReplies(final Vertx vertx, final VertxTestContext testContext) {
		final String instanceName = "cometd3";
		this.deployPipeline(vertx, this.listenerConfig(instanceName, "test:cometd3"))
				.onComplete(testContext.succeeding(mock -> {
					TestUtilities.startAllListeners(vertx);
					// Let the flow reach the long-poll loop, then stop
					vertx.setTimer(1000, timer -> {
						final String stopAddress = Constants.BUS_START_STOP + Constants.DELIMITER + instanceName;
						vertx.eventBus().request(stopAddress, Constants.MESSAGE_STOP)
								.onComplete(testContext.succeeding(reply -> testContext.verify(() -> {
									assertEquals(Constants.MESSAGE_STOP, reply.body());
									testContext.completeNow();
								})));
					});
				}));
	}
}
