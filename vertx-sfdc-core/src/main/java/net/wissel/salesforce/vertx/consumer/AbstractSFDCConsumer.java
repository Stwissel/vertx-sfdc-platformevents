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

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.wissel.salesforce.vertx.AbstractSFDCVerticle;
import net.wissel.salesforce.vertx.SFDCVerticle;
import net.wissel.salesforce.vertx.config.ConsumerConfig;

/**
 * @author swissel
 *
 */
public abstract class AbstractSFDCConsumer extends AbstractSFDCVerticle {

	protected MessageConsumer<JsonObject> consumer = null;
	private ConsumerConfig consumerConfig = null;

	@Override
	public SFDCVerticle startListening() {
		this.logger.info("Start listening:" + this.getClass().getName());
		// Listen on the event bus
		final EventBus eb = this.vertx.eventBus();
		this.consumer = eb.consumer(this.getConsumerConfig().getEventBusAddress());
		this.logger.info(this.getClass().getName() + " listening on " + this.consumer.address());
		this.consumer.handler(this::processIncoming);
		// Done
		this.listening = true;
		return this;
	}

	@Override
	public SFDCVerticle stopListening(final Future<Void> stopListenFuture) {
		this.logger.info("Stop listening:" + this.getClass().getName());
		this.listening = false;
		if (this.consumer == null) {
			stopListenFuture.complete();
		} else {
			this.consumer.unregister(res -> {
				stopListenFuture.complete();
			});
		}
		return this;
	}

	protected ConsumerConfig getConsumerConfig() {
		if (this.consumerConfig == null) {
			this.consumerConfig = this.config().mapTo(ConsumerConfig.class);
		}
		return this.consumerConfig;
	}

	/**
	 * What to do with actual arriving data
	 *
	 * @param incomingData
	 *            the payload in an envelope
	 */
	protected abstract void processIncoming(final Message<JsonObject> incomingData);

}
