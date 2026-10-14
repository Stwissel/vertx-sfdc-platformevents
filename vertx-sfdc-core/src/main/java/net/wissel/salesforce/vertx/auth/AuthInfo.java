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
package net.wissel.salesforce.vertx.auth;

/**
 * The outcome of a successful login: which Salesforce instance to talk to
 * and the session token to present on each request.
 *
 * Instances are immutable and travel the event bus via {@link AuthInfoCodec}
 *
 * @author swissel
 *
 */
public class AuthInfo {
	/**
	 * Host name of the Salesforce instance this session belongs to
	 */
	public final String serverName;
	/**
	 * Session token to send as the authorization header
	 */
	public final String sessionToken;
	
	/**
	 * Captures the result of one successful login
	 *
	 * @param server host name of the Salesforce instance
	 * @param token  session token obtained from the login
	 */
	public AuthInfo(final String server, final String token) {
		this.serverName = server;
		this.sessionToken = token;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.valueOf(this.serverName)+"\n"+String.valueOf(this.sessionToken);
	}
	
	
}
