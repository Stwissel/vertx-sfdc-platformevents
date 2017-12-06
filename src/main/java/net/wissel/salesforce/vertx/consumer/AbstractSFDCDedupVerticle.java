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
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import net.wissel.salesforce.vertx.Constants;

/**
 * A Dedup Verticle consumes an incoming message and tries to figure out if
 * that had been sent before. If that's the case the message is dropped
 * if not, then it is forwarded to its final destination retrieved from the header
 * 
 * @author stw
 *
 */
public abstract class AbstractSFDCDedupVerticle extends AbstractSDFCConsumer {

	@Override
	protected void addRoutes(final Router router) {
		// We don't add routes for this type of verticle
	}

	@Override
	protected void processIncoming(final Message<JsonObject> incomingData) {
		final MultiMap headers = incomingData.headers();
		final String finalDestination = headers.get(Constants.BUS_FINAL_DESTINATION);
		if (finalDestination != null) {
			final JsonObject j = incomingData.body();
			Future<Void> duplicate = Future.future(f -> {
				if (f.succeeded()) {
					// Forwarding it to where it came from
					this.getVertx().eventBus().send(finalDestination, j);
				} else {
					this.logger.info("Dropped duplicate Object:"+String.valueOf(j));
				}
			});
			// The duplicate check happens here!
			this.checkForDuplicate(duplicate, j);
		} else {
			this.logger.fatal(new Exception("Incoming message without final destination"+incomingData.toString()));
		}

	}

	/**
	 * Call to check if there is an actual duplicate
	 * @param failIfDuplicate Future that needs to fail if it is a duplicate, succeed if not
	 * @param messageBody the incoming message body to be checked
	 */
	protected abstract void checkForDuplicate(final Future<Void> failIfDuplicate, final JsonObject messageBody);

}
