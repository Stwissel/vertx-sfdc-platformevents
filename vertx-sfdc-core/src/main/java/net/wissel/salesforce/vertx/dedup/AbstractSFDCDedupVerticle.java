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

import java.util.List;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.wissel.salesforce.vertx.AbstractSFDCVerticle;
import net.wissel.salesforce.vertx.Constants;
import net.wissel.salesforce.vertx.SFDCVerticle;
import net.wissel.salesforce.vertx.config.DedupConfig;

/**
 * A Dedup Verticle consumes an incoming message and tries to figure out if that
 * had been sent before. If that's the case the message is dropped if not, then
 * it is forwarded to its final destination retrieved from the header
 *
 * @author stw
 *
 */
public abstract class AbstractSFDCDedupVerticle extends AbstractSFDCVerticle {

	protected MessageConsumer<JsonObject> dedupConsumer = null;
	private DedupConfig dedupConfig = null;

	@Override
	public SFDCVerticle startListening() {
		this.logger.info("Start listening:" + this.getClass().getName());
		// Listen on the event bus
		final EventBus eb = this.getVertx().eventBus();
		this.dedupConsumer = eb.consumer(Constants.BUS_DEDUP_PREFIX + this.getDedupConfig().getInstanceName());
		this.logger.info(this.getClass().getName() + " listening on " + this.dedupConsumer.address());
		this.dedupConsumer.handler(this::processIncoming);
		// Done
		this.listening = true;
		return this;
	}

	@Override
	public SFDCVerticle stopListening(final Future<Void> stopListenFuture) {
		this.logger.info("Stop listening:" + this.getClass().getName());
		this.listening = false;
		if (this.dedupConsumer == null) {
			stopListenFuture.complete();
		} else {
			this.dedupConsumer.unregister(res -> {
				stopListenFuture.complete();
			});
		}
		return this;
	}

	/**
	 ** Actual routine that check for "duplication". Could be anything, depending on use case.
	 * The future fails when a duplicate is found and succeeds when it is not.
	 * This allows for async execution
	 * 
	 * @param failIfDuplicate
	 *            Future that needs to fail if it is a duplicate, succeed if not
	 * @param messageBody
	 *            the incoming message body to be checked
	 */
	protected abstract void checkForDuplicate(final Future<Void> failIfDuplicate, final JsonObject messageBody);

	/**
	 * Retrieve the configuration provided on startup
	 * @return DedupConfig
	 */
	protected DedupConfig getDedupConfig() {
		if (this.dedupConfig == null) {
			this.dedupConfig = this.config().mapTo(DedupConfig.class);
		}
		return this.dedupConfig;
	}

	/**
	 * Check incoming messages for final destinations and then forward them if they are not
	 * duplicates
	 * 
	 * @param incomingData received from event bus
	 */
	protected void processIncoming(final Message<JsonObject> incomingData) {
		final MultiMap headers = incomingData.headers();
		final List<String> finalDestination = headers.getAll(Constants.BUS_FINAL_DESTINATION);
		if ((finalDestination != null) && !finalDestination.isEmpty()) {
			final JsonObject j = incomingData.body();
			// Forwarding the original headers
			final DeliveryOptions dOpts = new DeliveryOptions();
			dOpts.setHeaders(headers);
			final Future<Void> duplicate = Future.future(f -> {
				if (f.succeeded()) {
					// Forwarding it to where it should go
					final EventBus eb = this.getVertx().eventBus();
					finalDestination.forEach(d -> {
						eb.send(d, j, dOpts);
					});
				} else {
					this.logger.info("Dropped duplicate Object:" + String.valueOf(j));
				}
			});
			// The duplicate check happens here!
			this.checkForDuplicate(duplicate, j);
		} else {
			this.logger.fatal(new Exception("Incoming message without final destination" + incomingData.toString()));
		}

	}

}
