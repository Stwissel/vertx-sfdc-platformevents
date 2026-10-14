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
package net.wissel.salesforce.vertx.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.wissel.salesforce.vertx.Constants;
import net.wissel.salesforce.vertx.EchoServer;
import net.wissel.salesforce.vertx.TestUtilities;
import net.wissel.salesforce.vertx.auth.Basic;
import net.wissel.salesforce.vertx.config.AuthConfig;
import net.wissel.salesforce.vertx.config.ConsumerConfig;

/**
 * RestConsumer against a local echo endpoint: plain post, retry
 * behavior and the optional mustache transformation
 */
@ExtendWith(VertxExtension.class)
public class RestConsumerTest {

	private static final String BUS_ADDRESS = "test:restconsumer";

	/**
	 * Deploys Basic auth plus a RestConsumer pointing at the echo server,
	 * sends the start signal and waits for the consumer to be ready
	 */
	private Future<EchoServer> deployPipeline(final Vertx vertx, final ConsumerConfig consumerConfig) {
		TestUtilities.registerAuthCodec(vertx);

		final AuthConfig authConfig = new AuthConfig();
		authConfig.setAuthName("basic");
		authConfig.setServerURL("the.server.example.com");
		authConfig.setSfdcUser("User");
		authConfig.setSfdcPassword("pwd");

		final EchoServer echo = new EchoServer();
		return echo.start(vertx)
				.compose(server -> {
					consumerConfig.setUrl("http://localhost:" + echo.actualPort() + "/submission");
					return vertx.deployVerticle(new Basic(),
							new DeploymentOptions().setConfig(JsonObject.mapFrom(authConfig)));
				})
				.compose(id -> vertx.deployVerticle(new RestConsumer(),
						new DeploymentOptions().setConfig(JsonObject.mapFrom(consumerConfig))))
				.compose(id -> {
					TestUtilities.startAllListeners(vertx);
					// Give the start signal and the auth fetch a beat
					return vertx.timer(250).map(echo);
				});
	}

	private ConsumerConfig consumerConfig() {
		final ConsumerConfig config = new ConsumerConfig();
		config.setAuthName("basic");
		config.setEventBusAddress(BUS_ADDRESS);
		config.setInstanceName("restConsumer");
		config.addParameter("interval", "200");
		config.addParameter("maxRetryCount", "2");
		return config;
	}

	@Test
	public void postsIncomingMessageWithAuthHeader(final Vertx vertx, final VertxTestContext testContext) {
		final JsonObject payload = new JsonObject().put("payload", "data").put("number", 7);
		this.deployPipeline(vertx, this.consumerConfig())
				.onComplete(testContext.succeeding(echo -> {
					vertx.eventBus().publish(BUS_ADDRESS, payload);
					this.awaitReceived(vertx, echo, 1, () -> testContext.verify(() -> {
						final EchoServer.ReceivedRequest request = echo.received().get(0);
						assertEquals("POST", request.method());
						assertEquals("/submission", request.path());
						assertEquals(payload, new JsonObject(request.body()));
						assertTrue(request.headers().get(Constants.AUTH_HEADER).startsWith("Basic "));
						testContext.completeNow();
					}));
				}));
	}

	@Test
	public void failedPostIsRetried(final Vertx vertx, final VertxTestContext testContext) {
		final JsonObject payload = new JsonObject().put("payload", "retry me");
		this.deployPipeline(vertx, this.consumerConfig())
				.onComplete(testContext.succeeding(echo -> {
					echo.failNextRequests(1);
					vertx.eventBus().publish(BUS_ADDRESS, payload);
					// First attempt fails, the retry timer resends
					this.awaitReceived(vertx, echo, 2, () -> testContext.verify(() -> {
						assertEquals(payload, new JsonObject(echo.received().get(1).body()));
						testContext.completeNow();
					}));
				}));
	}

	@Test
	public void mustacheTemplateTransformsBody(final Vertx vertx, final VertxTestContext testContext) {
		final ConsumerConfig config = this.consumerConfig();
		config.addParameter(Constants.PARAM_STYLESHEET, "src/test/resources/resttransform.mustache");
		this.deployPipeline(vertx, config)
				.onComplete(testContext.succeeding(echo -> {
					vertx.eventBus().publish(BUS_ADDRESS, new JsonObject().put("payload", "shiny"));
					this.awaitReceived(vertx, echo, 1, () -> testContext.verify(() -> {
						assertTrue(echo.received().get(0).body().contains("EVENT:shiny"));
						testContext.completeNow();
					}));
				}));
	}

	@Test
	public void consumerWithoutAuthPostsWithoutAuthHeader(final Vertx vertx, final VertxTestContext testContext) {
		final String busAddress = "test:restconsumer-noauth";
		final ConsumerConfig config = new ConsumerConfig();
		// Deliberately no authName - getAuthInfo then never fills authInfo
		config.setEventBusAddress(busAddress);
		config.setInstanceName("restConsumerNoAuth");
		config.addParameter("interval", "200");
		config.addParameter("maxRetryCount", "2");

		final EchoServer echo = new EchoServer();
		echo.start(vertx)
				.compose(server -> {
					config.setUrl("http://localhost:" + echo.actualPort() + "/submission");
					return vertx.deployVerticle(new RestConsumer(),
							new DeploymentOptions().setConfig(JsonObject.mapFrom(config)));
				})
				.compose(id -> {
					TestUtilities.startAllListeners(vertx);
					return vertx.timer(250).map(echo);
				})
				.onComplete(testContext.succeeding(started -> {
					vertx.eventBus().publish(busAddress, new JsonObject().put("payload", "noauth"));
					// Dereferencing the absent AuthInfo used to throw here,
					// dropping the payload without scheduling a retry
					this.awaitReceived(vertx, echo, 1, () -> testContext.verify(() -> {
						assertNull(echo.received().get(0).headers().get(Constants.AUTH_HEADER));
						testContext.completeNow();
					}));
				}));
	}

	/**
	 * Polls until the echo server has seen the expected number of requests
	 */
	private void awaitReceived(final Vertx vertx, final EchoServer echo, final int count, final Runnable then) {
		if (echo.received().size() >= count) {
			then.run();
		} else {
			vertx.setTimer(50, timer -> this.awaitReceived(vertx, echo, count, then));
		}
	}
}
