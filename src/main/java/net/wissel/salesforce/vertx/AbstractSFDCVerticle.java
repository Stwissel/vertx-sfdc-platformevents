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
package net.wissel.salesforce.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Verticle Blueprint with some default behaviors we use in this application
 * like the undeploy command
 * 
 * @author swissel
 *
 */
public class AbstractSFDCVerticle extends AbstractVerticle {

	protected boolean shuttingDown = false;
	protected boolean shutdowCompleted = false;
	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	/**
	 * @see io.vertx.core.AbstractVerticle#start(io.vertx.core.Future)
	 */
	@Override
	public void start(Future<Void> startFuture) throws Exception {

		// Listen to the eventbus for start/stop commands
		EventBus eb = this.getVertx().eventBus();
		String stopAddress = Constants.BUS_START_STOP + Constants.DELIMITER + this.getClass().getName();

		// Listen to the start message
		eb.consumer(Constants.BUS_START_STOP, message -> {
			String payload = String.valueOf(message.body());
			if (payload.equals(Constants.MESSAGE_START)) {
				// Ready to start
				this.startListening();
			}
		});

		// Listen to the stop message
		eb.consumer(stopAddress, message -> {
			String payload = String.valueOf(message.body());
			if (payload.equals(Constants.MESSAGE_STOP)) {
				Future<Void> stopListenFuture = Future.future();
				stopListenFuture.setHandler(result -> {
					if (result.succeeded()) {
						message.reply(Constants.MESSAGE_STOP);
					} else {
						this.logger.error(result.cause());
						message.fail(500, result.cause().getMessage());
					}
				});
				this.stopListening(stopListenFuture);
			}
		});

		// TODO: Remove this eventually
		// And we are done
		startFuture.complete();
	}

	// TODO: make this abstact
	private void stopListening(Future<Void> stopListenFuture) {
		System.out.println("Stop listening:" + this.getClass().getName());
		stopListenFuture.complete();
	}

	// TODO: make this abstract
	private void startListening() {
		System.out.println("Start listening:" + this.getClass().getName());
	}

	/**
	 * @see io.vertx.core.AbstractVerticle#stop(io.vertx.core.Future)
	 */
	@Override
	public void stop(Future<Void> stopFuture) throws Exception {
		System.out.println("Stopped verticle:" + this.getClass().getName());
		stopFuture.complete();
	}

}
