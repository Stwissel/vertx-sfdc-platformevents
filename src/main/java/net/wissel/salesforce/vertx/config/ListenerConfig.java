/** ========================================================================= *
 * Copyright (C)  2017, 2018 Salesforce Inc ( http://www.salesforce.com/ )    *
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

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Setup for a subscriber listening to a Salesforce platform emitter As of
 * Winter '18 there is only CometD, but that might change in future
 *
 * @author stw
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListenerConfig extends BaseConfig {

	/**
	 * Which authConfig will provide the actual server and session string
	 */
	private String authName;

	/**
	 * The subscription topic to use
	 */
	private String listenSubject;

	/**
	 * Destination(s) to forward incoming data to
	 */
	private Set<String> eventBusAddresses = new HashSet<String>();

	public void addEventBusAddress(final String address) {
		this.eventBusAddresses.add(address);
	}

	/**
	 * @return the authName
	 */
	public final String getAuthName() {
		return this.authName;
	}

	/**
	 * @return the eventBusAddresses
	 */
	public final Set<String> getEventBusAddresses() {
		return this.eventBusAddresses;
	}

	/**
	 * @return the listenSubject
	 */
	public final String getListenSubject() {
		return this.listenSubject;
	}

	/**
	 * @see net.wissel.salesforce.vertx.config.BaseConfig#getVerticleInstanceCount()
	 */
	@Override
	public int getVerticleInstanceCount() {
		// There only can be one listener or we get double eventa
		return 1;
	}

	/**
	 * @param authName
	 *            the authName to set
	 */
	public final void setAuthName(final String authName) {
		this.authName = authName;
	}

	/**
	 * @param eventBusAddresses
	 *            the eventBusAddresses to set
	 */
	public final void setEventBusAddresses(final Set<String> eventBusAddresses) {
		this.eventBusAddresses = eventBusAddresses;
	}

	/**
	 * @param listenSubject
	 *            the listenSubject to set
	 */
	public final void setListenSubject(final String listenSubject) {
		this.listenSubject = listenSubject;
	}

}
