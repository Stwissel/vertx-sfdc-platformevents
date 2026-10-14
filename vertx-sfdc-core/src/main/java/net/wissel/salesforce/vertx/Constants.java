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
package net.wissel.salesforce.vertx;

/**
 * Shared constant values: event bus addresses, message and config keys,
 * HTTP headers and the Salesforce endpoints this library talks to.
 *
 * Declared as an interface so the values can be referenced without
 * importing a class that could be instantiated
 *
 * @author stw
 */
public interface Constants {
	/**
	 * Port assumed for TLS connections when none is configured
	 */
	int TLS_PORT = 443;	
	/**
	 * Root path under which this application exposes its own REST API
	 */
	String API_ROOT = "/api";
	/**
	 * HTTP header carrying the Salesforce session token
	 */
	String AUTH_HEADER = "Authorization";
	/**
	 * Message header asking an auth verticle to discard its cached session
	 */
	String AUTH_RESET = "RESET";
	/**
	 * Path of the Salesforce SOAP login endpoint
	 */
	String AUTH_SOAP_LOGIN = "/services/Soap/u/41.0/";
	/**
	 * Classpath resource holding the SOAP login request template
	 */
	String AUTH_SOAP_TEMPLATE = "/logintemplate.xml";
	// Messagebus constants	
	/**
	 * Event bus address prefix for auth requests, completed by the auth name
	 */
	String BUS_AUTHREQUEST = "SFDC:Auth:";
    /**
     * Event bus address on which configuration updates are announced
     */
    String BUS_CONFIGUPDATE = "SFDC:Config";
	/**
	 * Event bus address prefix for dedup services, completed by the instance name
	 */
	String BUS_DEDUP_PREFIX = "SFDC:Dedup:";
	/**
	 * Message header naming where a message goes once dedup has cleared it
	 */
	String BUS_FINAL_DESTINATION = "SFDCFinalDestination";
	/**
	 * Event bus address carrying the start and stop commands
	 */
	String BUS_START_STOP = "SFDC:CommandLine";
	/**
	 * Config key naming the auth provider a verticle uses
	 */
	String CONFIG_AUTHNAME = "authName";
	/**
	 * Config key deciding whether a verticle starts on deployment
	 */
	String CONFIG_AUTOSTART = "autoStart";
	/**
	 * Config key holding a verticle instance name
	 */
	String CONFIG_INSTANCENAME = "instanceName";
	/**
	 * Environment variable that overrides the web server port
	 */
	String CONFIG_PORT = "PORT";
	/**
	 * HTTP header naming the content type
	 */
	String CONTENT_HEADER = "Content-Type";
	/**
	 * Content type used for JSON payloads
	 */
	String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";
	/**
	 * Auth verticle used when the configuration names none
	 */
	String DEFAULT_AUTH_VERTICLE = "net.wissel.salesforce.vertx.auth.SoapApi";
	/**
	 * Date format the Salesforce APIs expect
	 */
	String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	/**
	 * Listener verticle used when the configuration names none
	 */
	String DEFAULT_LISTENER = "net.wissel.salesforce.vertx.listener.CometD";
	/**
	 * Separator used when composing event bus addresses
	 */
	String DELIMITER = ":";
	/**
	 * Message header marking the signal sent on startup
	 */
	String MESSAGE_ISSTARTUP = "StartupMessage";
	/**
	 * Body of the command telling verticles to start listening
	 */
	String MESSAGE_START = "Rock it cowboys";
	/**
	 * Body of the command telling verticles to stop listening
	 */
	String MESSAGE_STOP = "Party is over";
	/**
	 * Default file name of the JSON configuration
	 */
	String OPTION_FILE_NAME = "SFDCOptions.json";
	/**
	 * Parameter naming a Mustache template used to transform a payload
	 */
	String PARAM_STYLESHEET = "template";
	/**
	 * Login host for production orgs
	 */
	String PRODUCTION = "login.salesforce.com";
	/**
	 * Login host for sandbox orgs
	 */
	String SANDBOX = "test.salesforce.com";
	/**
	 * Value a header carries to mean true
	 */
	String TRUESTRING = "True";
	/**
	 * CometD endpoint holding the long poll open
	 */
	String URL_CONNECT = "/cometd/41.0/connect";
	/**
	 * CometD endpoint that opens a session
	 */
	String URL_HANDSHAKE = "/cometd/43.0/handshake";
	/**
	 * CometD endpoint that subscribes to a channel
	 */
	String URL_SUBSCRIBE = "/cometd/43.0/subscribe";

}
