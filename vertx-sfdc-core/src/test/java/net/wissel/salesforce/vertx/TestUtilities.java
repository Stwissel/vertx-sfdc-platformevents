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

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import net.wissel.salesforce.vertx.auth.AuthInfo;
import net.wissel.salesforce.vertx.auth.AuthInfoCodec;

/**
 * Shared helpers for tests that deploy SFDC verticles standalone,
 * without the ApplicationStarter doing the plumbing
 */
public final class TestUtilities {

	/**
	 * Standalone deployments need the AuthInfo codec that
	 * ApplicationStarter registers in production
	 *
	 * @param vertx the test Vertx instance
	 */
	public static void registerAuthCodec(final Vertx vertx) {
		try {
			vertx.eventBus().registerDefaultCodec(AuthInfo.class, new AuthInfoCodec());
		} catch (final IllegalStateException ignored) {
			// Already registered on this Vertx instance - fine
		}
	}

	/**
	 * SFDC verticles deploy passive and wait for the start signal.
	 * This sends the same signal ApplicationStarter sends on startup
	 *
	 * @param vertx the test Vertx instance
	 */
	public static void startAllListeners(final Vertx vertx) {
		final DeliveryOptions delOps = new DeliveryOptions().addHeader(Constants.MESSAGE_ISSTARTUP,
				Constants.TRUESTRING);
		vertx.eventBus().publish(Constants.BUS_START_STOP, Constants.MESSAGE_START, delOps);
	}

	private TestUtilities() {
		// Helpers only
	}
}
