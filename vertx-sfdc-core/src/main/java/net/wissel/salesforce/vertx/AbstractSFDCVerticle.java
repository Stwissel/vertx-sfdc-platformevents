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
 * Verticle Blueprint with default behaviors we use in this application
 * - Startup and shutdown support
 * - ability to listen to a "start listening" command on the event bus
 * - abstract functions for starting and stopping listening on the Bus or
 *   the external API
 *   
 * @author swissel
 *
 */
public abstract class AbstractSFDCVerticle extends AbstractVerticle implements SFDCVerticle {

	protected boolean shuttingDown = false;
	protected boolean shutdownCompleted = false;
	protected boolean startupCompleted = false;
	protected boolean listening = false;
	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	/**
	 * @see io.vertx.core.AbstractVerticle#start(io.vertx.core.Future)
	 */
	@Override
	public void start(final Future<Void> startFuture) throws Exception {

		// Listen to the eventbus for start/stop commands
		final EventBus eb = this.getVertx().eventBus();
		// Where does the Verticle listen to for shutdown instructions
		final String stopAddress = Constants.BUS_START_STOP + Constants.DELIMITER + this.config().getString(Constants.CONFIG_INSTANCENAME, this.getClass().getName());

		// Listen to the start message
		eb.consumer(Constants.BUS_START_STOP, message -> {
			final boolean duringStartup = Constants.TRUESTRING
					.equals(message.headers().get(Constants.MESSAGE_ISSTARTUP));
			final String payload = String.valueOf(message.body());
			if (this.shouldVerticleStartListening(payload, duringStartup)) {
				// Ready to start
				this.startListening();
			}
		});

		// Listen to the stop message
		eb.consumer(stopAddress, message -> {
			final String payload = String.valueOf(message.body());
			if (payload.equals(Constants.MESSAGE_STOP)) {
				final Future<Void> stopListenFuture = Future.future();
				stopListenFuture.setHandler(result -> {
					if (result.succeeded()) {
						this.listening = false;
						message.reply(Constants.MESSAGE_STOP);
					} else {
						this.logger.error(result.cause());
						message.fail(500, result.cause().getMessage());
					}
				});
				this.stopListening(stopListenFuture);
			}
		});

		// And we are done
		startFuture.complete();
	}

	/**
	 * @see io.vertx.core.AbstractVerticle#stop(io.vertx.core.Future)
	 */
	@Override
	public void stop(final Future<Void> stopFuture) throws Exception {
		this.shuttingDown = true;
		final Future<Void> stopListenFuture = Future.future();
		stopListenFuture.setHandler(handler -> {
			this.shutdownCompleted = true;
			this.logger.info("Stopped verticle:" + this.getClass().getName());
			stopFuture.complete();
		});
		this.stopListening(stopListenFuture);
	}

	/**
	 * Decides if that verticle starts listening to whatever needs to be
	 * listened to. All Verticles get an initial listening command at startup
	 * which they might ignore
	 *
	 * @param payload
	 *            - the message
	 * @param duringStartup
	 *            - send during Startup phase
	 * @return
	 */
	private boolean shouldVerticleStartListening(final String payload, final boolean duringStartup) {
		boolean result = false;
		// We only consider further logic if the right message arrived
		if (payload.equals(Constants.MESSAGE_START)) {
			// If it isn't startup, we listen in any case
			if (this.startupCompleted) {
				result = true;
			} else {
				// The first message constitutes the startup
				// to we set that to false in any case
				this.startupCompleted = true;
				// Make sure we are not in shutdown already
				if (!this.shuttingDown && !this.shutdownCompleted) {
					result = this.config().getBoolean(Constants.CONFIG_AUTOSTART, true);
				}
			}

		}
		return result;
	}

	/**
	 * @see net.wissel.salesforce.vertx.SFDCVerticle#startListening()
	 */
	public abstract SFDCVerticle startListening();

	/**
	 * @see net.wissel.salesforce.vertx.SFDCVerticle#stopListening(io.vertx.core.Future)
	 */
	public abstract SFDCVerticle stopListening(final Future<Void> stopListenFuture);

}
