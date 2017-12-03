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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * @author swissel
 *
 */
@RunWith(VertxUnitRunner.class)
public class ApplicationStarterTest {

	private Vertx vertx;

	@Before
	public void setUp(final TestContext context) {
		this.vertx = Vertx.vertx();
		final DeploymentOptions dOpts = new DeploymentOptions();
		dOpts.setConfig(new JsonObject().put(Constants.OPTION_FILE_NAME, "simpletest.json"));
		this.vertx.deployVerticle(ApplicationStarter.class.getName(), dOpts, context.asyncAssertSuccess());
	}

	@After
	public void tearDown(final TestContext context) {
		this.vertx.close(context.asyncAssertSuccess());
	}

	/* Check if the API endpoint is listening */
	@Test
	public void testAPIListening(final TestContext context) {
		final Async async = context.async();
		final WebClientOptions wco = new WebClientOptions();
		wco.setUserAgent("SDFC VertX Unit Tester");
		wco.setTryUseCompression(true);
		final WebClient client = WebClient.create(this.vertx, wco);

		client.get("http://localhost:8044/api").send(result -> {
			if (result.succeeded()) {
				context.assertEquals(200, result.result().statusCode());
			} else {
				context.fail(result.cause());
			}
			async.complete();
		});
	}
}
