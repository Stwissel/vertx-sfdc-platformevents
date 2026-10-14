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

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * Consumer that writes whatever arrives to the log, which makes it handy
 * for checking that a pipeline delivers before a real consumer exists
 *
 * @author swissel
 *
 */
public class ConsoleConsumer extends AbstractSFDCConsumer {

	/**
	 * Creates the consumer; Vert.x instantiates it on deployment
	 */
	public ConsoleConsumer() {
		// Nothing to set up
	}

	/**
	 * Writes the incoming data to the log, formatted for reading
	 *
	 * @see net.wissel.salesforce.vertx.consumer.AbstractSFDCConsumer#processIncoming(io.vertx.core.eventbus.Message)
	 */
	@Override
	protected void processIncoming(final Message<JsonObject> incomingData) {
		final JsonObject body = incomingData.body();
		this.logger.info(body.encodePrettily());
	}

}
