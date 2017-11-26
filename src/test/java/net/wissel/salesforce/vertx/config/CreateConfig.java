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

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.wissel.salesforce.vertx.Constants;

/**
 * Creates a new configuration object and serializes it Demo of all options
 * 
 * @author stw
 *
 */
public class CreateConfig {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		CreateConfig.makeSample();

	}

	private static void makeSample() {
		final AppConfig a = new AppConfig();

		final AuthConfig auth = new AuthConfig();
		auth.setAuthName("default");
		auth.setVerticleName(Constants.PRODUCTION);
		auth.setServerURL(Constants.PRODUCTION);
		auth.setSfdcUser("User");
		auth.setSfdcPassword("pwd");
		auth.addParameter("key", "value");

		final ListenerConfig lc = new ListenerConfig();
		lc.setVerticleName(Constants.DEFAULT_LISTENER);
		lc.setAutoStart(true);
		lc.addEventBusAddress("SFDC.SampleEvents");
		lc.addEventBusAddress("SFDC.SampleEvents2");
		lc.setAuthName("default");
		lc.setListenSubject("/event/DataChange__e");

		final ConsumerConfig cc = new ConsumerConfig();
		cc.setAutoStart(true);
		cc.setEventBusAddress("SFDC.SampleEvents");
		cc.setDeployAsWorker(true);
		cc.setProvidesRouterExtension(true);
		cc.setVerticleName("net.wissel.salesforce.vertx.consumer.LogConsumer");

		final ConsumerConfig cc2 = new ConsumerConfig();
		cc2.setAutoStart(false);
		cc2.setEventBusAddress("SFDC.SampleEvents");
		cc2.setVerticleName("net.wissel.salesforce.vertx.consumer.webSocketConsumer");

		a.addAuthConfig(auth);
		a.addListenerConfig(lc);
		a.addConsumerConfig(cc);
		a.addConsumerConfig(cc2);
		a.addParameter("key", "value");
		a.addParameter("key2", "value");

		final JsonObject j = JsonObject.mapFrom(a);

		System.out.println(j.encodePrettily());

		final Vertx vertx = Vertx.vertx();
		vertx.fileSystem().writeFile("sampleconfig.json", j.toBuffer(), res -> {
			System.out.println(res);
			vertx.close();
			;
		});

	}

}
