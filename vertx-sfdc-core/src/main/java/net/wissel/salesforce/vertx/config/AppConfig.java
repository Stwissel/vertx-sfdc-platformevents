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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Data class holding the configuration settings for the entire application
 * consisting of: - Listener configuration: Name, Parameters, Bus addresses -
 * Consumer configuration: Destination, class, parameters, Bus addresses -
 * Authentication modules: Name, class, parameters
 * 
 * @author stw
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {

	/**
	 * Authentication providers to deploy
	 */
	public final Collection<AuthConfig> authConfigurations = new ArrayList<AuthConfig>();
	/**
	 * Listeners to deploy, which subscribe to incoming Salesforce events
	 */
	public final Collection<ListenerConfig> listenerConfigurations = new ArrayList<ListenerConfig>();
	/**
	 * Consumers that act on the data arriving on the event bus
	 */
	public final Collection<ConsumerConfig> consumerConfigurations = new ArrayList<ConsumerConfig>();
	/**
	 * Deduplication services that filter repeated messages
	 */
	public final Collection<DedupConfig> dedupConfigurations = new ArrayList<DedupConfig>();
	/**
	 * Any further verticles to deploy, keyed by class name,
	 * with their raw parameters as the value
	 */
	public final Map<String, Map<String, Object>> verticlesToLoad = new HashMap<String, Map<String, Object>>();
	/**
	 * Free-form parameters shared by the whole application
	 */
	public final Map<String, String> parameters = new HashMap<String, String>();
	/**
	 * Proxy host applied to every verticle that does not set its own
	 */
	public String proxy = null; // Default for entire Verticles if set
	/**
	 * Proxy port applied to every verticle that does not set its own
	 */
	public int proxyPort = 0;
	/**
	 * Port the built-in web server listens on
	 */
	public int port = 8044; // Our WebServer Port

	/**
	 * Creates an empty application configuration, which the JSON
	 * mapping then fills
	 */
	public AppConfig() {
		// Nothing to set up
	}

	/**
	 * Registers an authentication provider
	 * 
	 * @param authConf Auth config to add
	 * @return fluid AppConfig
	 */
	public final AppConfig addAuthConfig(final AuthConfig authConf) {
		this.authConfigurations.add(authConf);
		return this;
	}
	
	/**
	 * Registers a consumer
	 * 
	 * @param consConf Consumer config to add
	 * @return fluid AppConfig
	 */
	public final AppConfig addConsumerConfig(final ConsumerConfig consConf) {
		this.consumerConfigurations.add(consConf);
		return this;
	}
	
	/**
	 * Registers a deduplication service
	 * 
	 * @param ddConf Dedup config to add
	 * @return fluid AppConfig
	 */
	public final AppConfig addDedupConfig(final DedupConfig ddConf) {
		this.dedupConfigurations.add(ddConf);
		return this;
	}

	/**
	 * Registers a listener
	 * 
	 * @param listConf Listener config to add
	 * @return fluid AppConfig
	 */
	public final AppConfig addListenerConfig(final ListenerConfig listConf) {
		this.listenerConfigurations.add(listConf);
		return this;
	}

	/**
	 * Adds or replaces an application-wide parameter
	 * 
	 * @param key Parameter key
	 * @param value Parameter value
	 * @return fluid AppConfig
	 */
	public final AppConfig addParameter(final String key, final String value) {
		this.parameters.put(key, value);
		return this;
	}

	/**
	 * Registers a verticle that none of the other categories covers
	 * 
	 * @param vName Verticle Name
	 * @param params Parameter Object
	 * @return fluid AppConfig
	 */
	public final AppConfig addVerticleConfig(final String vName, final Map<String, Object> params) {
		this.verticlesToLoad.put(vName, params);
		return this;
	}

}
