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
package net.wissel.salesforce.vertx.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Data class holding the configuration settings for the entire application
 * consisting of: - Listener configuration: Name, Parameters, Bus addresses -
 * Consumer configuration: Destination, class, parameters, Bus addresses -
 * Authentication modules: Name, class, parameters
 * 
 * @author stw
 *
 */
public class AppConfig {

	public final Map<String, AuthConfig> authConfigurations = new HashMap<String, AuthConfig>();
	public final Collection<ListenerConfig> listenerConfigurations = new ArrayList<ListenerConfig>();
	public final Collection<ConsumerConfig> consumerConfigurations = new ArrayList<ConsumerConfig>();
	public final Map<String, Map<String, Object>> verticlesToLoad = new HashMap<String, Map<String, Object>>();
	public final Map<String, String> parameters = new HashMap<String, String>();
	public int port = 8044; // Our WebServer Port

	/* Convenience methods */
	public final void addAuthConfig(final AuthConfig authConf) {
		this.authConfigurations.put(authConf.getAuthName(), authConf);
	}

	public final void addConsumerConfig(final ConsumerConfig consConf) {
		this.consumerConfigurations.add(consConf);
	}

	public final void addListenerConfig(final ListenerConfig listConf) {
		this.listenerConfigurations.add(listConf);
	}

	public final void addParameter(final String key, final String value) {
		this.parameters.put(key, value);
	}

	public final void addVerticleConfig(final String vName, final Map<String, Object> params) {
		this.verticlesToLoad.put(vName, params);
	}

}
