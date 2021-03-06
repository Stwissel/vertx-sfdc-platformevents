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
public class DedupConfig extends BaseConfig {

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
	 * Port for external service
	 */
	private int port = 0;

	/**
	 * @return the port
	 */
	public int getPort() {
		return this.port;
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
	 * @param port
	 *            the port to set
	 * @return DedupConfig fluid
	 */
	public final DedupConfig setPort(final int port) {
		this.port = port;
		return this;
	}

	/**
	 * @param serverURL
	 *            the serverURL to set
	 * @return DedupConfig fluid
	 */
	public final DedupConfig setServerURL(final String serverURL) {
		this.serverURL = serverURL;
		return this;
	}

	/**
	 * @param passWord
	 *            the passWord to set
	 * @return DedupConfig fluid
	 */
	public final DedupConfig setSfdcPassword(final String passWord) {
		this.sfdcPassword = passWord;
		return this;
	}

	/**
	 * @param userName
	 *            the userName to set
	 * @return DedupConfig fluid
	 */
	public final DedupConfig setSfdcUser(final String userName) {
		this.sfdcUser = userName;
		return this;
	}
}
