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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.wissel.salesforce.vertx.Constants;
import net.wissel.salesforce.vertx.TestUtilities;
import net.wissel.salesforce.vertx.config.AuthConfig;

/**
 * SOAP login against a local mock of the Salesforce login endpoint
 */
@ExtendWith(VertxExtension.class)
public class SoapApiTest {

	private static final String GOOD_SOAP_RESPONSE = """
			<?xml version="1.0" encoding="UTF-8"?>
			<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
				xmlns="urn:partner.soap.sforce.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
				<soapenv:Body>
					<loginResponse>
						<result>
							<passwordExpired>false</passwordExpired>
							<serverUrl>https://testinstance.example/services/Soap/u/41.0/00D123</serverUrl>
							<sessionId>SESSION123</sessionId>
						</result>
					</loginResponse>
				</soapenv:Body>
			</soapenv:Envelope>
			""";

	private static final String EXPIRED_SOAP_RESPONSE = GOOD_SOAP_RESPONSE
			.replace("<passwordExpired>false</passwordExpired>", "<passwordExpired>true</passwordExpired>");

	private Future<AuthInfo> loginAgainstMock(final Vertx vertx, final String responseBody,
			final AtomicReference<String> requestBodyCapture) {
		TestUtilities.registerAuthCodec(vertx);
		return vertx.createHttpServer().requestHandler(request -> request.body().onSuccess(body -> {
			if (requestBodyCapture != null) {
				requestBodyCapture.set(body.toString());
			}
			request.response().putHeader("Content-Type", "text/xml").end(responseBody);
		})).listen(0).compose(server -> this.deployAndRequest(vertx, server));
	}

	private Future<AuthInfo> deployAndRequest(final Vertx vertx, final HttpServer server) {
		final AuthConfig config = new AuthConfig();
		config.setAuthName("soap");
		config.setServerURL("localhost");
		config.setSfdcUser("soap@example.com");
		config.setSfdcPassword("soapSecret");
		config.addParameter("port", String.valueOf(server.actualPort()));
		config.addParameter("ssl", "false");

		return vertx.deployVerticle(new SoapApi(), new DeploymentOptions().setConfig(JsonObject.mapFrom(config)))
				.compose(id -> vertx.eventBus().<AuthInfo>request(Constants.BUS_AUTHREQUEST + "soap", null))
				.map(reply -> reply.body());
	}

	@Test
	public void successfulLoginExtractsServerAndSession(final Vertx vertx, final VertxTestContext testContext) {
		final AtomicReference<String> requestBody = new AtomicReference<>();
		this.loginAgainstMock(vertx, GOOD_SOAP_RESPONSE, requestBody)
				.onComplete(testContext.succeeding(authInfo -> testContext.verify(() -> {
					assertEquals("testinstance.example", authInfo.serverName);
					assertEquals("SESSION123", authInfo.sessionToken);
					// The login body is the rendered mustache template
					assertTrue(requestBody.get().contains("soap@example.com"));
					assertTrue(requestBody.get().contains("soapSecret"));
					testContext.completeNow();
				})));
	}

	@Test
	public void expiredPasswordFailsLogin(final Vertx vertx, final VertxTestContext testContext) {
		this.loginAgainstMock(vertx, EXPIRED_SOAP_RESPONSE, null)
				.onComplete(testContext.failing(t -> testContext.completeNow()));
	}

	@Test
	public void garbageResponseFailsLogin(final Vertx vertx, final VertxTestContext testContext) {
		this.loginAgainstMock(vertx, "This is not XML at all { maybe json? }", null)
				.onComplete(testContext.failing(t -> testContext.completeNow()));
	}
}
