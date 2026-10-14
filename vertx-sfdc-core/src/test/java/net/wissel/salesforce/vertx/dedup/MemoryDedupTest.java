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
package net.wissel.salesforce.vertx.dedup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.wissel.salesforce.vertx.Constants;
import net.wissel.salesforce.vertx.TestUtilities;
import net.wissel.salesforce.vertx.config.DedupConfig;

/**
 * In-memory deduplication: the direct check, the bounded capacity that
 * evicts the oldest entry, and the bus flow that forwards new messages
 * while dropping the ones already seen
 */
@ExtendWith(VertxExtension.class)
public class MemoryDedupTest {

	// --- Direct unit tests of the dedup logic (no deployment needed) ---

	@Test
	public void duplicateIsDetected() {
		final MemoryDedup dedup = new MemoryDedup();
		final JsonObject message = new JsonObject().put("payload", "data");

		assertTrue(dedup.checkForDuplicate(message).succeeded());
		assertTrue(dedup.checkForDuplicate(message).failed());
	}

	// Each iteration needs a distinct message, so the allocation is deliberate.
	// Reusing one mutated JsonObject would couple this test to MemoryDedup
	// encoding the body immediately instead of retaining the reference.
	@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
	@Test
	public void capacityIsLimitedTo100Entries() {
		final MemoryDedup dedup = new MemoryDedup();
		final JsonObject first = new JsonObject().put("number", 0);
		assertTrue(dedup.checkForDuplicate(first).succeeded());

		// Push 100 more entries through - the first must get evicted
		for (int i = 1; i <= 100; i++) {
			assertTrue(dedup.checkForDuplicate(new JsonObject().put("number", i)).succeeded());
		}

		// No longer known, so not a duplicate anymore
		assertTrue(dedup.checkForDuplicate(first).succeeded());
	}

	// --- Bus flow: regression test for the broken forwarding ---

	@Test
	public void forwardsNewMessagesAndDropsDuplicates(final Vertx vertx, final VertxTestContext testContext) {
		final String instanceName = "memtest";
		final String finalDestination = "test:final";

		final DedupConfig config = new DedupConfig();
		config.setInstanceName(instanceName);
		config.setVerticleName(MemoryDedup.class.getName());

		final JsonObject messageA = new JsonObject().put("payload", "A");
		final JsonObject messageB = new JsonObject().put("payload", "B");
		final List<JsonObject> arrived = new ArrayList<>();

		vertx.eventBus().<JsonObject>consumer(finalDestination, message -> {
			arrived.add(message.body());
			if (arrived.size() == 2) {
				testContext.verify(() -> {
					// Duplicate A must have been dropped in between
					assertEquals(messageA, arrived.get(0));
					assertEquals(messageB, arrived.get(1));
					testContext.completeNow();
				});
			}
		});

		final DeliveryOptions withDestination = new DeliveryOptions()
				.addHeader(Constants.BUS_FINAL_DESTINATION, finalDestination);

		vertx.deployVerticle(new MemoryDedup(), new DeploymentOptions().setConfig(JsonObject.mapFrom(config)))
				.onComplete(testContext.succeeding(id -> {
					TestUtilities.startAllListeners(vertx);
					// Give the start signal a beat to register the dedup consumer
					vertx.setTimer(250, timer -> {
						final String address = Constants.BUS_DEDUP_PREFIX + instanceName;
						vertx.eventBus().send(address, messageA, withDestination);
						vertx.eventBus().send(address, messageA, withDestination);
						vertx.eventBus().send(address, messageB, withDestination);
					});
				}));
	}

	@Test
	public void messageWithoutDestinationIsNotForwarded(final Vertx vertx, final VertxTestContext testContext) {
		final String instanceName = "memtest2";
		final DedupConfig config = new DedupConfig();
		config.setInstanceName(instanceName);

		final List<JsonObject> arrived = new ArrayList<>();
		vertx.eventBus().<JsonObject>consumer("test:final2", message -> arrived.add(message.body()));

		vertx.deployVerticle(new MemoryDedup(), new DeploymentOptions().setConfig(JsonObject.mapFrom(config)))
				.onComplete(testContext.succeeding(id -> {
					TestUtilities.startAllListeners(vertx);
					vertx.setTimer(250, timer -> {
						// No final destination header - message gets logged and dropped
						vertx.eventBus().send(Constants.BUS_DEDUP_PREFIX + instanceName,
								new JsonObject().put("payload", "lost"));
						vertx.setTimer(250, inner -> testContext.verify(() -> {
							assertFalse(arrived.contains(new JsonObject().put("payload", "lost")));
							testContext.completeNow();
						}));
					});
				}));
	}
}
