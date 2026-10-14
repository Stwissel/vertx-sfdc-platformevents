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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.wissel.salesforce.vertx.Constants;
import net.wissel.salesforce.vertx.TestUtilities;
import net.wissel.salesforce.vertx.config.DedupConfig;

/**
 * A dedup check reports its verdict by failing the returned future, so an
 * unreachable dedup store looks exactly like a duplicate unless the two are
 * told apart. Dropping the message in that case loses the event, which is
 * what these tests guard against
 */
@ExtendWith(VertxExtension.class)
public class DedupFailureTest {

	/** Stands in for a dedup store that cannot be reached */
	private static class BrokenDedup extends AbstractSFDCDedupVerticle {
		@Override
		protected Future<Void> checkForDuplicate(final JsonObject messageBody) {
			return Future.failedFuture(new IllegalStateException("Dedup store unreachable"));
		}
	}

	/** Reports every message as a confirmed duplicate */
	private static class AlwaysDuplicateDedup extends AbstractSFDCDedupVerticle {
		@Override
		protected Future<Void> checkForDuplicate(final JsonObject messageBody) {
			return Future.failedFuture(new DuplicateMessageException("always"));
		}
	}

	/**
	 * Deploys the given dedup verticle, sends it one message carrying a final
	 * destination, and collects whatever arrives there
	 *
	 * @param vertx        the test Vertx instance
	 * @param dedup        the verticle under test
	 * @param instanceName its configured instance name
	 * @param arrived      collects the forwarded messages
	 * @param payload      the message to send
	 * @return a future completing once the message has had time to travel
	 */
	private Future<Void> runOneMessage(final Vertx vertx, final AbstractSFDCDedupVerticle dedup,
			final String instanceName, final List<JsonObject> arrived, final JsonObject payload) {
		final String finalDestination = "test:dedupfail:" + instanceName;
		final DedupConfig config = new DedupConfig();
		config.setInstanceName(instanceName);

		vertx.eventBus().<JsonObject>consumer(finalDestination, message -> arrived.add(message.body()));

		final DeliveryOptions withDestination = new DeliveryOptions()
				.addHeader(Constants.BUS_FINAL_DESTINATION, finalDestination);

		return vertx.deployVerticle(dedup, new DeploymentOptions().setConfig(JsonObject.mapFrom(config)))
				.compose(id -> {
					TestUtilities.startAllListeners(vertx);
					// Give the start signal a beat to register the consumer
					return vertx.timer(250);
				})
				.compose(started -> {
					vertx.eventBus().send(Constants.BUS_DEDUP_PREFIX + instanceName, payload, withDestination);
					// And a beat for the message to travel
					return vertx.timer(250);
				})
				.mapEmpty();
	}

	@Test
	public void storeFailureForwardsInsteadOfDropping(final Vertx vertx, final VertxTestContext testContext) {
		final JsonObject payload = new JsonObject().put("payload", "must survive");
		final List<JsonObject> arrived = new ArrayList<>();

		this.runOneMessage(vertx, new BrokenDedup(), "dedupBroken", arrived, payload)
				.onComplete(testContext.succeeding(v -> testContext.verify(() -> {
					// The check failed, so whether this is a duplicate is
					// unknown - the event must not be silently discarded
					assertTrue(arrived.contains(payload), "Event lost when the dedup store failed");
					assertEquals(1, arrived.size());
					testContext.completeNow();
				})));
	}

	@Test
	public void confirmedDuplicateIsStillDropped(final Vertx vertx, final VertxTestContext testContext) {
		final JsonObject payload = new JsonObject().put("payload", "seen before");
		final List<JsonObject> arrived = new ArrayList<>();

		this.runOneMessage(vertx, new AlwaysDuplicateDedup(), "dedupDupe", arrived, payload)
				.onComplete(testContext.succeeding(v -> testContext.verify(() -> {
					// A real duplicate is still dropped, as before
					assertTrue(arrived.isEmpty(), "Confirmed duplicate was forwarded");
					testContext.completeNow();
				})));
	}
}
