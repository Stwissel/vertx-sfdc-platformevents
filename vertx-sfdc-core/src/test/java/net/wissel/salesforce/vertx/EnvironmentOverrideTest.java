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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.wissel.salesforce.vertx.config.AppConfig;

/**
 * The environment override logic keeps credentials out of the config
 * file. These tests feed a fake environment map into the logic
 */
public class EnvironmentOverrideTest {

	private ApplicationStarter starter;

	@BeforeEach
	public void setup() {
		this.starter = new ApplicationStarter();
	}

	private JsonObject sampleConfig() {
		return new JsonObject()
				.put("authConfigurations", new JsonArray()
						.add(new JsonObject().put("authName", "mainAuth")))
				.put("listenerConfigurations", new JsonArray()
						.add(new JsonObject().put("authName", "mainAuth").put("listenSubject", "/event/X__e")))
				.put("consumerConfigurations", new JsonArray()
						.add(new JsonObject().put("authName", "mainAuth").put("eventBusAddress", "SFDC:x")))
				.put("dedupConfigurations", new JsonArray()
						.add(new JsonObject().put("authName", "redisAuth")));
	}

	@Test
	public void portOverrideReachesAppConfig() {
		final JsonObject config = this.sampleConfig();
		this.starter.addParametersFromEnvironment(config, Map.of("PORT", "9099"));

		// The override must survive the Jackson mapping into AppConfig
		final AppConfig appConfig = config.mapTo(AppConfig.class);
		assertEquals(9099, appConfig.port);
	}

	@Test
	public void unparseablePortIsIgnored() {
		final JsonObject config = this.sampleConfig();
		this.starter.addParametersFromEnvironment(config, Map.of("PORT", "not-a-number"));
		final AppConfig appConfig = config.mapTo(AppConfig.class);
		assertEquals(8044, appConfig.port);
	}

	@Test
	public void credentialsApplyToAuthAndDedupLists() {
		final JsonObject config = this.sampleConfig();
		this.starter.addParametersFromEnvironment(config, Map.of(
				"mainAuth_sfdcUser", "user@example.com",
				"mainAuth_sfdcPassword", "secret",
				"redisAuth_sfdcPassword", "redisSecret"));

		final JsonObject auth = config.getJsonArray("authConfigurations").getJsonObject(0);
		assertEquals("user@example.com", auth.getString("sfdcUser"));
		assertEquals("secret", auth.getString("sfdcPassword"));

		// Regression: the dedup list was unreachable due to a key typo
		final JsonObject dedup = config.getJsonArray("dedupConfigurations").getJsonObject(0);
		assertEquals("redisSecret", dedup.getString("sfdcPassword"));
	}

	@Test
	public void onlyProxySettingsApplyToListenersAndConsumers() {
		final JsonObject config = this.sampleConfig();
		this.starter.addParametersFromEnvironment(config, Map.of(
				"mainAuth_Proxy", "proxy.example.com",
				"mainAuth_ProxyPort", "3128",
				"mainAuth_sfdcPassword", "mustNotLeakHere"));

		final JsonObject listener = config.getJsonArray("listenerConfigurations").getJsonObject(0);
		assertEquals("proxy.example.com", listener.getString("Proxy"));
		assertEquals("3128", listener.getString("ProxyPort"));
		// Credentials only land on auth/dedup entries
		assertNull(listener.getString("sfdcPassword"));

		final JsonObject consumer = config.getJsonArray("consumerConfigurations").getJsonObject(0);
		assertEquals("proxy.example.com", consumer.getString("Proxy"));
	}

	@Test
	public void entriesWithoutAuthNameAreUntouched() {
		final JsonObject config = new JsonObject()
				.put("listenerConfigurations", new JsonArray().add(new JsonObject().put("listenSubject", "/event/X__e")));
		this.starter.addParametersFromEnvironment(config, Map.of("null_Proxy", "proxy.example.com"));

		final JsonObject listener = config.getJsonArray("listenerConfigurations").getJsonObject(0);
		assertFalse(listener.containsKey("Proxy"));
	}
}
