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

import java.util.Base64;

import io.vertx.core.Future;
import net.wissel.salesforce.vertx.config.AuthConfig;

/**
 * Provide auth info for basic authentication - which is just the header
 * out of username and password encoded - should cover lots of cases
 * 
 * @author swissel
 *
 */
public class Basic extends AbstractAuth{

	@Override
	protected void login(Future<AuthInfo> futureAuthinfo) {
		AuthConfig ac = this.getAuthConfig();
		String userCredentials = String.valueOf(ac.getSfdcUser())+":"+String.valueOf(ac.getSfdcPassword());
		String token = "Basic " + Base64.getEncoder().encodeToString(userCredentials.getBytes());
		AuthInfo ai = new AuthInfo(ac.getServerURL(), token);
		this.setCachedAuthInfo(ai);
		futureAuthinfo.complete(ai);
	}

}
