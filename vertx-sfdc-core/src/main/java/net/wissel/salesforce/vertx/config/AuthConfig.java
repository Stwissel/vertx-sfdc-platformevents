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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Stores configuration options for an authorization provider The authentication
 * provider provides the value of the Authorization string typically added to
 * the HTTP header for proper access to protected resources
 *
 * @author stw
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthConfig extends BaseConfig {

	/**
	 * Unique name to identify the configuration It's possible to have multiple
	 * AuthConfig pointing to the same server for different authorization. Forms
	 * part of its EventBus address
	 */
	private String authName;

	/**
	 * URL pointing to the authentication destination Special case: URL could be
	 * "PRODUCTION" or "SANDBOX" as shortcut for the Salesforce URLs
	 */
	private String serverURL;

	/**
	 * Username, typically supplied by env_variable Variable
	 * is[authName]_userName
	 */
	private String sfdcUser = null;

	/**
	 * Password, typically supplied by env_variable
	 */
	private String sfdcPassword = null;

	/**
	 * Token used for oAuth, typically supplied by env_variable Variable
	 * is[authName]_consumerToken
	 */
	private String consumerToken = null;

	/**
	 * secret used for oAuth, typically supplied by env_variable Variable
	 * is[authName]_consumerSecret
	 */
	private String consumerSecret = null;

	/**
	 * @return the authName
	 */
	public final String getAuthName() {
		return this.authName;
	}

	/**
	 * @return the consumerSecret
	 */
	public final String getConsumerSecret() {
		return this.consumerSecret;
	}

	/**
	 * @return the consumerToken
	 */
	public final String getConsumerToken() {
		return this.consumerToken;
	}

	/**
	 * @return the serverURL
	 */
	public final String getServerURL() {
		return this.serverURL;
	}

	/**
	 * @return the sfdcPassword
	 */
	public final String getSfdcPassword() {
		return this.sfdcPassword;
	}

	/**
	 * @return the sfdcUser
	 */
	public final String getSfdcUser() {
		return this.sfdcUser;
	}

	/**
	 * @param authName
	 *            the authName to set
	 * @return AuthConfig fluid
	 */
	public final AuthConfig setAuthName(final String authName) {
		this.authName = authName;
		return this;
	}

	/**
	 * @param consumerSecret
	 *            the consumerSecret to set
	 * @return AuthConfig fluid
	 */
	public final AuthConfig setConsumerSecret(final String consumerSecret) {
		this.consumerSecret = consumerSecret;
		return this;
	}

	/**
	 * @param consumerToken
	 *            the consumerToken to set
	 * @return AuthConfig fluid
	 */
	public final AuthConfig setConsumerToken(final String consumerToken) {
		this.consumerToken = consumerToken;
		return this;
	}

	/**
	 * @param serverURL
	 *            the serverURL to set
	 * @return AuthConfig fluid
	 */
	public final AuthConfig setServerURL(final String serverURL) {
		this.serverURL = serverURL;
		return this;
	}

	/**
	 * @param passWord
	 *            the passWord to set
	 * @return AuthConfig fluid
	 */
	public final AuthConfig setSfdcPassword(final String passWord) {
		this.sfdcPassword = passWord;
		return this;
	}

	/**
	 * @param userName
	 *            the userName to set
	 * @return AuthConfig fluid
	 */
	public final AuthConfig setSfdcUser(final String userName) {
		this.sfdcUser = userName;
		return this;
	}
}
