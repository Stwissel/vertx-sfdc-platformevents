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

	public void addParameter(final String key, final String value) {
		this.parameters.put(key, value);
	}

	/**
	 * @return the parameters
	 */
	public Map<String, String> getParameters() {
		return this.parameters;
	}
	
	/**
	 * Was an additional parameter provided in the configuration
	 * @param paraName
	 * @return
	 */
	public boolean hasParameter(final String paraName) {
		return (this.parameters != null && this.parameters.containsKey(paraName));
	}
	
	/** 
	 * Returns a specific parameter if existing
	 * @param paraName name of the parameter
	 * @return the value or null
	 */
	public String getParameter(final String paraName) {
		return (this.parameters == null) ? null : this.parameters.get(paraName);
	}
	
	/** 
	 * Returns a specific parameter if existing
	 * @param paraName name of the parameter
	 * @return the value or the default value
	 */
	public String getParameter(final String paraName, final String defaultValue) {
		return (this.parameters == null) ? null : ((this.parameters.get(paraName) == null) ? defaultValue : this.parameters.get(paraName));
	}

	/**
	 * @return the proxy
	 */
	public final String getProxy() {
		return this.proxy;
	}

	/**
	 * @return the proxyPort
	 */
	public final int getProxyPort() {
		return this.proxyPort;
	}

	/**
	 * @return the verticleInstanceCount
	 */
	public int getVerticleInstanceCount() {
		return this.verticleInstanceCount;
	}

	/**
	 * @return the verticleName
	 */
	public String getVerticleName() {
		return this.verticleName;
	}

	/**
	 * @return the autoStart
	 */
	public boolean isAutoStart() {
		return this.autoStart;
	}

	/**
	 * @return the deployAsWorker
	 */
	public boolean isDeployAsWorker() {
		return this.deployAsWorker;
	}

	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return this.enabled;
	}

	/**
	 * @return the providesRouterExtension
	 */
	public boolean isProvidesRouterExtension() {
		return this.providesRouterExtension;
	}

	/**
	 * @param autoStart
	 *            the autoStart to set
	 */
	public BaseConfig setAutoStart(final boolean autoStart) {
		this.autoStart = autoStart;
		return this;
	}

	/**
	 * @param deployAsWorker
	 *            the deployAsWorker to set
	 */
	public BaseConfig setDeployAsWorker(final boolean deployAsWorker) {
		this.deployAsWorker = deployAsWorker;
		return this;
	}

	/**
	 * @param enabled
	 *            the enabled to set
	 */
	public BaseConfig setEnabled(final boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	/**
	 * @param parameters
	 *            the parameters to set
	 */
	public BaseConfig setParameters(final Map<String, String> parameters) {
		this.parameters = parameters;
		return this;
	}

	/**
	 * @param providesRouterExtension
	 *            the providesRouterExtension to set
	 */
	public BaseConfig setProvidesRouterExtension(final boolean providesRouterExtension) {
		this.providesRouterExtension = providesRouterExtension;
		return this;
	}

	/**
	 * @param proxy
	 *            the proxy to set
	 */
	public BaseConfig setProxy(final String proxy) {
		this.proxy = proxy;
		return this;
	}

	/**
	 * @param proxy
	 *            the proxy to set
	 */
	public BaseConfig setProxyFromAppConfig(final AppConfig appConfig) {
		if (this.proxy == null && appConfig.proxy != null) {
			this.proxy = appConfig.proxy;
		}

		if (this.proxyPort == 0 && appConfig.proxyPort != 0) {
			this.proxyPort = appConfig.proxyPort;
		}
		
		return this;
	}

	/**
	 * @param proxyPort
	 *            the proxyPort to set
	 */
	public BaseConfig setProxyPort(final int proxyPort) {
		this.proxyPort = proxyPort;
		return this;
	}

	/**
	 * @param verticleInstanceCount
	 *            the verticleInstanceCount to set
	 */
	public BaseConfig setVerticleInstanceCount(final int verticleInstanceCount) {
		this.verticleInstanceCount = verticleInstanceCount;
		return this;
	}

	/**
	 * @param verticleName
	 *            the verticleName to set
	 */
	public BaseConfig setVerticleName(final String verticleName) {
		this.verticleName = verticleName;
		return this;
	}

}
