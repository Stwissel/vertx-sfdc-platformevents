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

public interface Constants {
	int TLS_PORT = 443;	
	String API_ROOT = "/api";
	String AUTH_HEADER = "Authorization";
	String AUTH_RESET = "RESET";
	String AUTH_SOAP_LOGIN = "/services/Soap/u/41.0/";
	String AUTH_SOAP_TEMPLATE = "/logintemplate.xml";
	// Messagebus constants	
	String BUS_AUTHREQUEST = "SFDC:Auth:";
	String BUS_FINAL_DESTINATION = "SFDCFinalDestination";
	String BUS_START_STOP = "SFDC:CommandLine";
	String CONFIG_AUTHNAME = "authName";
	String CONFIG_AUTOSTART = "autoStart";
	String CONFIG_PORT = "Port";
	String CONTENT_HEADER = "Content-Type";
	String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";
	String DEFAULT_AUTH_VERTICLE = "net.wissel.salesforce.vertx.auth.SoapApi";
	String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	String DEFAULT_LISTENER = "net.wissel.salesforce.vertx.listener.CometD";
	String DELIMITER = ":";
	String MESSAGE_ISSTARTUP = "StartupMessage";
	String MESSAGE_START = "Rock it cowboys";
	String MESSAGE_STOP = "Party is over";
	String OPTION_FILE_NAME = "SFDCOptions.json";
	String PARAM_STYLESHEET = "stylesheet.mustache";
	String PRODUCTION = "login.salesforce.com";
	String SANDBOX = "test.salesforce.com";
	String TRUESTRING = "True";
	String URL_CONNECT = "/cometd/41.0/connect";
	String URL_HANDSHAKE = "/cometd/41.0/handshake";
	String URL_SUBSCRIBE = "/cometd/41.0/subscribe";
}
