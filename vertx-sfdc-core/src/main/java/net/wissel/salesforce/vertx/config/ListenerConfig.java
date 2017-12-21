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
	 * Should the listener send its data to the dedup service or directly to the
	 * consumers
	 */
	private boolean useDedupService = false;

	/**
	 * The address for the deduplication Service Will be used when
	 * useDedupService is true;
	 */
	private String eventBusDedupAddress;

	/**
	 * Destination(s) to forward incoming data to
	 */
	private Set<String> eventBusAddresses = new HashSet<String>();

	/**
	 *
	 * @param address
	 *            new Eventbus address to use
	 */
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
	 * @return the eventBusDedupAddress
	 */
	public final String getEventBusDedupAddress() {
		return this.eventBusDedupAddress;
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
	 * @return the useDedupService
	 */
	public final boolean isUseDedupService() {
		return this.useDedupService;
	}

	/**
	 * @param authName
	 *            the authName to set
	 * @return fluid ListenerConfig
	 */
	public final ListenerConfig setAuthName(final String authName) {
		this.authName = authName;
		return this;
	}

	/**
	 * @param eventBusAddresses
	 *            the eventBusAddresses to set
	 * @return fluid ListenerConfig
	 */
	public final ListenerConfig setEventBusAddresses(final Set<String> eventBusAddresses) {
		this.eventBusAddresses = eventBusAddresses;
		return this;
	}

	/**
	 * @param eventBusDedupAddress
	 *            the eventBusDedupAddress to set
	 * @return fluid ListenerConfig
	 */
	public final ListenerConfig setEventBusDedupAddress(final String eventBusDedupAddress) {
		this.eventBusDedupAddress = eventBusDedupAddress;
		return this;
	}

	/**
	 * @param listenSubject
	 *            the listenSubject to set
	 * @return fluid ListenerConfig
	 */
	public final ListenerConfig setListenSubject(final String listenSubject) {
		this.listenSubject = listenSubject;
		return this;
	}

	/**
	 * @param useDedupService
	 *            the useDedupService to set
	 * @return fluid ListenerConfig
	 */
	public final ListenerConfig setUseDedupService(final boolean useDedupService) {
		this.useDedupService = useDedupService;
		return this;
	}

}
