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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Common methods of the configuration classes
 *
 * @author stw
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseConfig {

	/**
	 * Class that implements that listener, one per type
	 *
	 */
	private String verticleName;

	/**
	 * Name that uniquely defines this instance(s) name
	 */
	private String instanceName;

	/**
	 * How many to deploy
	 */
	private int verticleInstanceCount = 1;

	/**
	 * Should it be deployed as worker
	 */
	private boolean deployAsWorker = false;

	/**
	 * Shall the loader start the verticle after deployment
	 */
	private boolean autoStart = true;

	/**
	 * Is the verticle allow to listen/act?
	 */
	private boolean enabled = true;

	/**
	 * Does the verticle provide a router extension - pretty hack, we might want
	 * to let that one go sometime
	 */
	private boolean providesRouterExtension = false;

	/**
	 * Name or IP address of an eventual proxy server to use
	 */
	private String proxy = null;

	/**
	 * Port for an eventual proxy server to use
	 */
	private int proxyPort = 0;

	/**
	 * Other parameters
	 */
	private Map<String, String> parameters = new HashMap<String, String>();

	/**
	 * Creates an empty configuration, which the JSON mapping then fills
	 */
	public BaseConfig() {
		// Nothing to set up
	}

	/**
	 * Adds or replaces one of the free-form parameters
	 *
	 * @param key   the parameter name
	 * @param value the value to store under that name
	 */
	public void addParameter(final String key, final String value) {
		this.parameters.put(key, value);
	}

	/**
	 * Gets the name that uniquely identifies this instance
	 *
	 * @return the instanceName
	 */
	public String getInstanceName() {
		return this.instanceName;
	}

	/**
	 * Returns a specific parameter if existing
	 *
	 * @param paraName
	 *            name of the parameter
	 * @return the value or null
	 */
	public String getParameter(final String paraName) {
		return (this.parameters == null) ? null : this.parameters.get(paraName);
	}

	/**
	 * Returns a specific parameter if existing
	 *
	 * @param paraName
	 *            name of the parameter
	 * @param defaultValue
	 *            result if parameter doesn't exist
	 * @return the value or the default value
	 */
	public String getParameter(final String paraName, final String defaultValue) {
		return (this.parameters == null) ? null
				: ((this.parameters.get(paraName) == null) ? defaultValue : this.parameters.get(paraName));
	}

	/**
	 * Gets every free-form parameter supplied in the configuration
	 *
	 * @return the parameters
	 */
	public Map<String, String> getParameters() {
		return this.parameters;
	}

	/**
	 * Gets the host name or IP of the proxy server, if one is configured
	 *
	 * @return the proxy
	 */
	public final String getProxy() {
		return this.proxy;
	}

	/**
	 * Gets the port of the proxy server, if one is configured
	 *
	 * @return the proxyPort
	 */
	public final int getProxyPort() {
		return this.proxyPort;
	}

	/**
	 * Gets how many instances of the verticle get deployed
	 *
	 * @return the verticleInstanceCount
	 */
	public int getVerticleInstanceCount() {
		return this.verticleInstanceCount;
	}

	/**
	 * Gets the class name of the verticle implementing this entry
	 *
	 * @return the verticleName
	 */
	public String getVerticleName() {
		return this.verticleName;
	}

	/**
	 * Was an additional parameter provided in the configuration
	 *
	 * @param paraName
	 *            name to check
	 * @return true/false if parameter exists
	 */
	public boolean hasParameter(final String paraName) {
		return ((this.parameters != null) && this.parameters.containsKey(paraName));
	}

	/**
	 * Reports whether the loader starts the verticle right after deployment
	 *
	 * @return the autoStart
	 */
	public boolean isAutoStart() {
		return this.autoStart;
	}

	/**
	 * Reports whether the verticle is deployed as a worker
	 *
	 * @return the deployAsWorker
	 */
	public boolean isDeployAsWorker() {
		return this.deployAsWorker;
	}

	/**
	 * Reports whether the verticle is allowed to listen and act
	 *
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return this.enabled;
	}

	/**
	 * Reports whether the verticle contributes a router extension
	 *
	 * @return the providesRouterExtension
	 */
	public boolean isProvidesRouterExtension() {
		return this.providesRouterExtension;
	}

	/**
	 * Sets whether the loader starts the verticle right after deployment
	 *
	 * @param autoStart
	 *            the autoStart to set
	 * @return BaseConfig fluid
	 */
	public BaseConfig setAutoStart(final boolean autoStart) {
		this.autoStart = autoStart;
		return this;
	}

	/**
	 * Sets whether the verticle is deployed as a worker
	 *
	 * @param deployAsWorker
	 *            the deployAsWorker to set
	 * @return BaseConfig fluid
	 */
	public BaseConfig setDeployAsWorker(final boolean deployAsWorker) {
		this.deployAsWorker = deployAsWorker;
		return this;
	}

	/**
	 * Sets whether the verticle is allowed to listen and act
	 *
	 * @param enabled
	 *            the enabled to set
	 * @return BaseConfig fluid
	 */
	public BaseConfig setEnabled(final boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	/**
	 * Sets the name that uniquely identifies this instance
	 *
	 * @param instanceName
	 *            the instanceName to set
	 * @return fluid - self
	 */
	public BaseConfig setInstanceName(final String instanceName) {
		this.instanceName = instanceName;
		return this;
	}

	/**
	 * Replaces every free-form parameter
	 *
	 * @param parameters
	 *            the parameters to set
	 * @return BaseConfig fluid
	 */
	public BaseConfig setParameters(final Map<String, String> parameters) {
		this.parameters = parameters;
		return this;
	}

	/**
	 * Sets whether the verticle contributes a router extension
	 *
	 * @param providesRouterExtension
	 *            the providesRouterExtension to set
	 * @return BaseConfig fluid
	 */
	public BaseConfig setProvidesRouterExtension(final boolean providesRouterExtension) {
		this.providesRouterExtension = providesRouterExtension;
		return this;
	}

	/**
	 * Sets the host name or IP of the proxy server to use
	 *
	 * @param proxy
	 *            the proxy to set
	 * @return BaseConfig fluid
	 */
	public BaseConfig setProxy(final String proxy) {
		this.proxy = proxy;
		return this;
	}

	/**
	 * Inherits proxy host and port from the application configuration, leaving any value already set here untouched
	 *
	 * @param appConfig
	 *            the appConfig with proxy information
	 * @return BaseConfig fluid
	 */
	public BaseConfig setProxyFromAppConfig(final AppConfig appConfig) {
		if ((this.proxy == null) && (appConfig.proxy != null)) {
			this.proxy = appConfig.proxy;
		}

		if ((this.proxyPort == 0) && (appConfig.proxyPort != 0)) {
			this.proxyPort = appConfig.proxyPort;
		}

		return this;
	}

	/**
	 * Sets the port of the proxy server to use
	 *
	 * @param proxyPort
	 *            the proxyPort to set
	 * @return BaseConfig fluid
	 */
	public BaseConfig setProxyPort(final int proxyPort) {
		this.proxyPort = proxyPort;
		return this;
	}

	/**
	 * Sets how many instances of the verticle get deployed
	 *
	 * @param verticleInstanceCount
	 *            the verticleInstanceCount to set
	 * @return BaseConfig fluid
	 */
	public BaseConfig setVerticleInstanceCount(final int verticleInstanceCount) {
		this.verticleInstanceCount = verticleInstanceCount;
		return this;
	}

	/**
	 * Sets the class name of the verticle implementing this entry
	 *
	 * @param verticleName
	 *            the verticleName to set
	 * @return BaseConfig fluid
	 */
	public BaseConfig setVerticleName(final String verticleName) {
		this.verticleName = verticleName;
		return this;
	}

}
