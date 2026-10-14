/** ========================================================================= *
 * Copyright (C)  2017 Salesforce Inc ( http://www.salesforce.com/ )          *
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

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class Runner {

	static Logger logger = LoggerFactory.getLogger(Runner.class);

	public static void runVerticle(final String verticleID, final boolean debugMode) {

		// That's just for IDE testing...
		final Consumer<Vertx> runner = vertx -> {
			try {
				final DeploymentOptions depOpt = new DeploymentOptions();
				vertx.deployVerticle(verticleID, depOpt)
						.onSuccess(id -> Runner.logger.info("{} deployed as {}", verticleID, id))
						.onFailure(t -> Runner.logger.error("Deployment failed for {}", verticleID, t));
			} catch (final Throwable t) {
				Runner.logger.error("Could not run verticle", t);
			}
		};

		final VertxOptions options = new VertxOptions();
		if (Runner.isDebug(debugMode)) {
			options.setBlockedThreadCheckInterval(1000 * 60 * 60);
		}

		final Vertx vertx = Vertx.vertx(options);
		runner.accept(vertx);
	}

	private static boolean isDebug(boolean debugMode) {
		// Debug was given as parameter or there's an environment variable
		// set to true so we go debugging
		return debugMode || Boolean.parseBoolean(System.getenv("Debug"));
	}
}
