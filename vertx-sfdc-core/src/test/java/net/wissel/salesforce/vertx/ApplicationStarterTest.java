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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * End to end: ApplicationStarter reads the test config, deploys the
 * configured verticles and serves the admin API on a random port
 */
@ExtendWith(VertxExtension.class)
public class ApplicationStarterTest {

	@Test
	public void startsVerticlesAndServesApi(final Vertx vertx, final VertxTestContext testContext) {
		final ApplicationStarter starter = new ApplicationStarter();
		final DeploymentOptions options = new DeploymentOptions()
				.setConfig(new JsonObject().put(Constants.OPTION_FILE_NAME, "src/test/resources/simpletest.json"));

		vertx.deployVerticle(starter, options)
				.compose(id -> {
					// The start future completing implies the bind worked
					final int port = starter.getListeningPort();
					testContext.verify(() -> assertTrue(port > 0, "Server should listen on a real port"));
					return WebClient.create(vertx).get(port, "localhost", Constants.API_ROOT).send();
				})
				.onComplete(testContext.succeeding(response -> testContext.verify(() -> {
					assertEquals(200, response.statusCode());
					final JsonObject body = response.bodyAsJsonObject();
					assertNotNull(body.getString("RunningSince"));
					assertNotNull(body.getJsonObject("Routes"));
					// Exactly ONE verticle from simpletest.json - this is the
					// regression check for the historic double deployment
					assertEquals(1, body.getJsonArray("Verticles").size());
					testContext.completeNow();
				})));
	}
}
