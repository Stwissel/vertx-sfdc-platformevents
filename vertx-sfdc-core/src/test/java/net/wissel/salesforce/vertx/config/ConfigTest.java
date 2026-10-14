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
package net.wissel.salesforce.vertx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

/**
 * JSON round trips of the config classes - this is what
 * ApplicationStarter does with every verticle configuration
 */
public class ConfigTest {

	@Test
	public void appConfigDefaults() {
		final AppConfig ac = new AppConfig();
		assertEquals(8044, ac.port);
		assertNull(ac.proxy);
		assertEquals(0, ac.proxyPort);
		assertTrue(ac.authConfigurations.isEmpty());
	}

	@Test
	public void appConfigRoundTrip() {
		final AppConfig source = new AppConfig();
		source.port = 9999;
		source.proxy = "proxy.example.com";
		source.proxyPort = 8080;
		final AuthConfig auth = new AuthConfig();
		auth.setAuthName("mainAuth").setServerURL("login.salesforce.com");
		auth.setSfdcUser("user@example.com").setSfdcPassword("secret");
		source.addAuthConfig(auth);

		final JsonObject json = JsonObject.mapFrom(source);
		final AppConfig restored = json.mapTo(AppConfig.class);

		assertEquals(9999, restored.port);
		assertEquals("proxy.example.com", restored.proxy);
		assertEquals(8080, restored.proxyPort);
		assertEquals(1, restored.authConfigurations.size());
		final AuthConfig restoredAuth = restored.authConfigurations.iterator().next();
		assertEquals("mainAuth", restoredAuth.getAuthName());
		assertEquals("login.salesforce.com", restoredAuth.getServerURL());
		assertEquals("user@example.com", restoredAuth.getSfdcUser());
	}

	@Test
	public void unknownJsonKeysAreIgnored() {
		final JsonObject json = new JsonObject()
				.put("port", 1234)
				.put("futureSetting", "no idea yet")
				.put("anotherUnknown", new JsonObject().put("nested", true));

		final AppConfig restored = json.mapTo(AppConfig.class);

		assertEquals(1234, restored.port);
	}

	@Test
	public void listenerConfigRoundTripAndSingleInstance() {
		final ListenerConfig source = new ListenerConfig();
		source.setAuthName("mainAuth").setListenSubject("/event/DataChange__e");
		source.setUseDedupService(true).setEventBusDedupAddress("SFDC:Dedup:redis");
		source.addEventBusAddress("SFDC:thing");
		source.setInstanceName("listener1");

		final JsonObject json = JsonObject.mapFrom(source);
		final ListenerConfig restored = json.mapTo(ListenerConfig.class);

		assertEquals("mainAuth", restored.getAuthName());
		assertEquals("/event/DataChange__e", restored.getListenSubject());
		assertTrue(restored.isUseDedupService());
		assertEquals("SFDC:Dedup:redis", restored.getEventBusDedupAddress());
		assertTrue(restored.getEventBusAddresses().contains("SFDC:thing"));
		// Listeners never scale out - that would mean double events
		assertEquals(1, restored.getVerticleInstanceCount());
	}

	@Test
	public void consumerConfigRoundTrip() {
		final ConsumerConfig source = new ConsumerConfig();
		source.setAuthName("restAuth").setEventBusAddress("SFDC:incoming").setUrl("https://example.com/post");
		source.addParameter("interval", "500");

		final JsonObject json = JsonObject.mapFrom(source);
		final ConsumerConfig restored = json.mapTo(ConsumerConfig.class);

		assertEquals("restAuth", restored.getAuthName());
		assertEquals("SFDC:incoming", restored.getEventBusAddress());
		assertEquals("https://example.com/post", restored.getUrl());
		assertEquals("500", restored.getParameter("interval"));
		assertEquals("fallback", restored.getParameter("missing", "fallback"));
		assertTrue(restored.hasParameter("interval"));
		assertFalse(restored.hasParameter("missing"));
	}

	@Test
	public void dedupConfigRoundTrip() {
		final DedupConfig source = new DedupConfig();
		source.setServerURL("redis.example.com").setSfdcUser("ignored").setSfdcPassword("redisSecret");
		source.setPort(6380);
		source.setInstanceName("redisDedup");

		final JsonObject json = JsonObject.mapFrom(source);
		final DedupConfig restored = json.mapTo(DedupConfig.class);

		assertEquals("redis.example.com", restored.getServerURL());
		assertEquals("redisSecret", restored.getSfdcPassword());
		assertEquals(6380, restored.getPort());
		assertEquals("redisDedup", restored.getInstanceName());
	}

	@Test
	public void proxySettingsInheritFromAppConfig() {
		final AppConfig app = new AppConfig();
		app.proxy = "global.proxy";
		app.proxyPort = 3128;

		final ConsumerConfig withOwnProxy = new ConsumerConfig();
		withOwnProxy.setProxy("own.proxy");
		withOwnProxy.setProxyPort(9999);
		withOwnProxy.setProxyFromAppConfig(app);
		assertEquals("own.proxy", withOwnProxy.getProxy());
		assertEquals(9999, withOwnProxy.getProxyPort());

		final ConsumerConfig withoutProxy = new ConsumerConfig();
		withoutProxy.setProxyFromAppConfig(app);
		assertEquals("global.proxy", withoutProxy.getProxy());
		assertEquals(3128, withoutProxy.getProxyPort());
	}
}
