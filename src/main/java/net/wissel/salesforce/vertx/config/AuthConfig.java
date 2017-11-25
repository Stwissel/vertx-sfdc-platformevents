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

import java.util.HashMap;
import java.util.Map;

/**
 * Stores configuration options for an authorization provider
 * The authentication provider provides the value of the
 * Authorization string typically added to the HTTP header for
 * proper access to protected resources
 * 
 * @author stw
 *
 */
public class AuthConfig {
	
	/**
	 * Unique name to identify the configuration
	 * It's possible to have multiple AuthConfig
	 * pointing to the same server for different
	 * authorization. Forms part of its EventBus address
	 */
	private String authName;
	
	/**
	 * Name / Class of the verticle providing the service
	 */
	private String verticleName;
	
	/**
	 * URL pointing to the authentication destination
	 * Special case: URL could be "PRODUCTION" or "SANDBOX"
	 * as shortcut for the Salesforce URLs
	 */
	private String serverURL;
	
	/**
	 * Username, typically supplied by env_variable
	 * Variable is[authName]_userName
	 */
	private String userName = null;
	
	/**
	 * Password, typically supplied by env_variable
	 */
	private String passWord = null;
		
	/** 
	 * Other parameters like sessions, consumer secrets etc.
	 * for various types of auth module
	 */
	private Map<String, String> parameters = new HashMap<String, String>();

	/**
	 * @param parameters the parameters to set
	 */
	public final void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	/**
	 * @return the authName
	 */
	public final String getAuthName() {
		return authName;
	}

	/**
	 * @param authName the authName to set
	 */
	public final void setAuthName(String authName) {
		this.authName = authName;
	}

	/**
	 * @return the verticleName
	 */
	public final String getVerticleName() {
		return verticleName;
	}

	/**
	 * @param verticleName the verticleName to set
	 */
	public final void setVerticleName(String verticleName) {
		this.verticleName = verticleName;
	}

	/**
	 * @return the serverURL
	 */
	public final String getServerURL() {
		return serverURL;
	}

	/**
	 * @param serverURL the serverURL to set
	 */
	public final void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}

	/**
	 * @return the userName
	 */
	public final String getUserName() {
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public final void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the passWord
	 */
	public final String getPassWord() {
		return passWord;
	}

	/**
	 * @param passWord the passWord to set
	 */
	public final void setPassWord(String passWord) {
		this.passWord = passWord;
	}

	/**
	 * @return the parameters
	 */
	public final Map<String, String> getParameters() {
		return parameters;
	}

	public void addParameter(String key, String value) {
		this.parameters.put(key, value);		
	}
	
}
