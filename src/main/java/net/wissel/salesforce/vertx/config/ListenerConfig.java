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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Setup for a subscriber listening to a Salesforce platform emitter As of
 * Winter '18 there is only CometD, but that might change in future
 *
 * @author stw
 *
 */
public class ListenerConfig {

	/**
	 * How will incoming data be processes SEND: to one receiving Verticle
	 * PUBLISH: any verticle care to listen
	 *
	 * @author stw
	 *
	 */
	public enum ForwardAs {
		SEND, PUBLISH
	}

	/**
	 * Which authConfig will provide the actual server and session string
	 */
	private String authName;

	/**
	 * Class that implements that listener, mostly the same for all
	 *
	 */
	private String verticleName;

	/**
	 * The subscription topic to use
	 */
	private String listenSubject;

	/**
	 * Destination(s) to forward incoming data to
	 */
	private Set<String> eventBusAddresses = new HashSet<String>();

	/**
	 * Send or publish incoming data?
	 */
	private ForwardAs forwardAs = ForwardAs.SEND;

	/**
	 * Shall the loader start the verticle after deployment
	 */
	private boolean autoStart = true;

	/**
	 * Is the verticle allow to listen/act?
	 */
	private boolean enabled = true;

	/**
	 * Other parameters
	 */
	private Map<String, String> parameters = new HashMap<String, String>();

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
	 * @return the forwardAs
	 */
	public final ForwardAs getForwardAs() {
		return this.forwardAs;
	}

	/**
	 * @return the listenSubject
	 */
	public final String getListenSubject() {
		return this.listenSubject;
	}

	/**
	 * @return the parameters
	 */
	public final Map<String, String> getParameters() {
		return this.parameters;
	}

	/**
	 * @return the verticleName
	 */
	public final String getVerticleName() {
		return this.verticleName;
	}

	/**
	 * @return the autoStart
	 */
	public final boolean isAutoStart() {
		return this.autoStart;
	}

	/**
	 * @return the enabled
	 */
	public final boolean isEnabled() {
		return this.enabled;
	}

	/**
	 * @param authName
	 *            the authName to set
	 */
	public final void setAuthName(final String authName) {
		this.authName = authName;
	}

	/**
	 * @param autoStart
	 *            the autoStart to set
	 */
	public final void setAutoStart(final boolean autoStart) {
		this.autoStart = autoStart;
	}

	/**
	 * @param enabled
	 *            the enabled to set
	 */
	public final void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * @param eventBusAddresses
	 *            the eventBusAddresses to set
	 */
	public final void setEventBusAddresses(final Set<String> eventBusAddresses) {
		this.eventBusAddresses = eventBusAddresses;
	}

	/**
	 * @param forwardAs
	 *            the forwardAs to set
	 */
	public final void setForwardAs(final ForwardAs forwardAs) {
		this.forwardAs = forwardAs;
	}

	/**
	 * @param listenSubject
	 *            the listenSubject to set
	 */
	public final void setListenSubject(final String listenSubject) {
		this.listenSubject = listenSubject;
	}

	/**
	 * @param parameters
	 *            the parameters to set
	 */
	public final void setParameters(final Map<String, String> parameters) {
		this.parameters = parameters;
	}

	/**
	 * @param verticleName
	 *            the verticleName to set
	 */
	public final void setVerticleName(final String verticleName) {
		this.verticleName = verticleName;
	}

}
