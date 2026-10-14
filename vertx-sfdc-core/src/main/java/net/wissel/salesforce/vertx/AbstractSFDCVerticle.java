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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

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
public abstract class AbstractSFDCVerticle extends VerticleBase implements SFDCVerticle {

	protected boolean shuttingDown = false;
	protected boolean shutdownCompleted = false;
	protected boolean startupCompleted = false;
	protected boolean listening = false;
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * VerticleBase has no getVertx() accessor, subclasses rely on one
	 *
	 * @return the Vertx instance this verticle is deployed in
	 */
	protected Vertx getVertx() {
		return this.vertx;
	}

	/**
	 * @see io.vertx.core.VerticleBase#start()
	 */
	@Override
	public Future<?> start() throws Exception {

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
				this.stopListening().onComplete(result -> {
					if (result.succeeded()) {
						this.listening = false;
						message.reply(Constants.MESSAGE_STOP);
					} else {
						this.logger.error("Stop listening failed", result.cause());
						message.fail(500, result.cause().getMessage());
					}
				});
			}
		});

		// Listen to incoming configuration changes
		eb.consumer(Constants.BUS_CONFIGUPDATE, message -> {
		    final JsonObject newConfig = (JsonObject) message.body();
		    Utils.mergeObjects(this.config(), newConfig, JsonMergeOptions.REMOVE_MISSING);
		});

		// And we are done
		return Future.succeededFuture();
	}

	/**
	 * @see io.vertx.core.VerticleBase#stop()
	 */
	@Override
	public Future<?> stop() throws Exception {
		this.shuttingDown = true;
		// A failing stopListening must not fail undeploy, hence transform
		return this.stopListening().transform(result -> {
			this.shutdownCompleted = true;
			this.logger.info("Stopped verticle: {}", this.getClass().getName());
			return Future.succeededFuture();
		});
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
	 * @see net.wissel.salesforce.vertx.SFDCVerticle#stopListening()
	 */
	public abstract Future<Void> stopListening();

}
