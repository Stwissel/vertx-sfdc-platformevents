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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Configuration for consumers listening on event bus messages to act on them
 *
 * @author stw
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsumerConfig extends BaseConfig {

	/**
	 * Destination to listen to
	 */
	private String eventBusAddress;

	/**
	 * Which authConfig will provide authorization (if any)
	 */
	private String authName = null;

	/**
	 * Endpoint the consumer serves can be REST, SOAP, SOCKET
	 */
	private String url;

	/**
	 * @return the authName
	 */
	public final String getAuthName() {
		return this.authName;
	}

	/**
	 * @return the eventBusAddress
	 */
	public final String getEventBusAddress() {
		return this.eventBusAddress;
	}

	/**
	 *
	 * @return The URL the consumer serves, e.g. REST, WebSocket etc
	 */
	public final String getUrl() {
		return this.url;
	}

	/**
	 * @param authName
	 *            the authName to set
	 */
	public final ConsumerConfig setAuthName(final String authName) {
		this.authName = authName;
		return this;
	}

	/**
	 * @param eventBusAddress
	 *            the eventBusAddress to set
	 */
	public final ConsumerConfig setEventBusAddress(final String eventBusAddress) {
		this.eventBusAddress = eventBusAddress;
		return this;
	}

	public final ConsumerConfig setUrl(final String newURL) {
		this.url = newURL;
		return this;
	}

}
