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
package net.wissel.salesforce.vertx.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.wissel.salesforce.vertx.Constants;
import net.wissel.salesforce.vertx.TestUtilities;
import net.wissel.salesforce.vertx.config.AuthConfig;

/**
 * Basic authentication answers an auth request on the bus with a
 * ready-to-use Base64 header
 */
@ExtendWith(VertxExtension.class)
public class BasicAuthTest {

	@Test
	public void basicAuthDeliversBase64Header(final Vertx vertx, final VertxTestContext testContext) {
		TestUtilities.registerAuthCodec(vertx);

		final AuthConfig config = new AuthConfig();
		config.setAuthName("basic");
		config.setServerURL("the.server.example.com");
		config.setSfdcUser("User");
		config.setSfdcPassword("pwd");

		final DeploymentOptions options = new DeploymentOptions().setConfig(JsonObject.mapFrom(config));
		vertx.deployVerticle(new Basic(), options)
				.compose(id -> vertx.eventBus().<AuthInfo>request(Constants.BUS_AUTHREQUEST + "basic", null))
				.onComplete(testContext.succeeding(reply -> testContext.verify(() -> {
					final AuthInfo authInfo = reply.body();
					final String expected = "Basic "
							+ Base64.getEncoder().encodeToString("User:pwd".getBytes(StandardCharsets.UTF_8));
					assertEquals(expected, authInfo.sessionToken);
					assertEquals("the.server.example.com", authInfo.serverName);
					testContext.completeNow();
				})));
	}
}
