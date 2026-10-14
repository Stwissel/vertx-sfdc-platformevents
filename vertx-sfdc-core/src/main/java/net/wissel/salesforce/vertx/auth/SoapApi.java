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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import net.wissel.salesforce.vertx.Constants;

/**
 * Authentication through the Salesforce SOAP login endpoint, using a user
 * name and password.
 *
 * The request body is rendered from a template on the classpath, and the
 * response yields the instance host name and session token
 *
 * @author stw
 */
public class SoapApi extends AbstractAuth {
	/**
	 * Creates the provider; Vert.x instantiates it on deployment
	 */
	public SoapApi() {
		// Nothing to set up
	}


	@Override
	protected Future<AuthInfo> login() {
		if (this.shuttingDown || this.shutdownCompleted) {
			this.shutdownCompleted = true;
			return Future.failedFuture("Auth disrupted by stop command");
		}
		final WebClientOptions wco = new WebClientOptions();
		final String proxyHost = this.getAuthConfig().getProxy();
		final int proxyPort = this.getAuthConfig().getProxyPort();
		if ((proxyHost != null) && (proxyPort > 0)) {
			final ProxyOptions po = new ProxyOptions();
			wco.setProxyOptions(po);
			po.setHost(proxyHost).setPort(proxyPort);
		}
		wco.setUserAgent("SDFC VertX Authenticator");
		wco.setDecompressionSupported(true);
		final WebClient authClient = WebClient.create(this.getVertx(), wco);
		final Buffer body = this.getAuthBody(this.getAuthConfig().getSfdcUser(),
				this.getAuthConfig().getSfdcPassword());
		// port/ssl are configurable through parameters, so tests can run
		// against a local mock server; production defaults stay 443/TLS
		final int port = Integer.parseInt(
				this.getAuthConfig().getParameter("port", String.valueOf(Constants.TLS_PORT)));
		final boolean useSsl = Boolean.parseBoolean(this.getAuthConfig().getParameter("ssl", "true"));
		return authClient.post(port, this.getAuthConfig().getServerURL(), Constants.AUTH_SOAP_LOGIN)
				.putHeader("Content-Type", "text/xml").ssl(useSsl).putHeader("SOAPAction", "Login")
				.putHeader("PrettyPrint", "Yes").sendBuffer(body)
				.compose(this::resultOfAuthentication);
	}

	private AuthInfo extractAuthInfoFromBody(final Buffer body) {
		AuthInfo result = null;
		String instanceHostName = null;
		String sessionId = null;

		try {
			// Setup the XML document
			final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder builder = builderFactory.newDocumentBuilder();
			final ByteArrayInputStream in = new ByteArrayInputStream(body.getBytes());
			final Document xmlDocument = builder.parse(in);
			in.close();
			final XPath xPath = XPathFactory.newInstance().newXPath();
			xPath.setNamespaceContext(new NamespaceContext() {
				@Override
				public String getNamespaceURI(final String prefix) {
					String result;
					if ("soapenv".equals(prefix)) {
						result = "http://schemas.xmlsoap.org/soap/envelope/";
					} else if ("xsi".equals(prefix)) {
						result = "http://www.w3.org/2001/XMLSchema-instance";
					} else {
						result = "urn:partner.soap.sforce.com";
					}
					return result;
				}

				@Override
				public String getPrefix(final String arg0) {
					return null;
				}

				@Override
				public Iterator<String> getPrefixes(final String arg0) {
					return null;
				}
			});

			// Now retrieve passwordExpired, serverURL sessionID
			final String pwdExpiredQuery = "//passwordExpired";
			final String serverQuery = "//serverUrl";
			final String sessionIdQuery = "//sessionId";

			// Check for expired password
			final String pwdNode = String.valueOf(xPath.evaluate(pwdExpiredQuery, xmlDocument, XPathConstants.STRING));
			if (!"false".equalsIgnoreCase(pwdNode)) {
				this.logger.error("Credential password expired");
			} else {
				// Next step, find the actual server
				final String serverNode = String
						.valueOf(xPath.evaluate(serverQuery, xmlDocument, XPathConstants.STRING));
				if (!"null".equalsIgnoreCase(serverNode)) {
					// Now chop off protocol and url parts
					final String candidate = serverNode.substring(serverNode.indexOf("/") + 2);
					instanceHostName = candidate.substring(0, candidate.indexOf("/"));
				}
				// Grab the sessionID, needed for authorization later on
				final String sessionNode = String
						.valueOf(xPath.evaluate(sessionIdQuery, xmlDocument, XPathConstants.STRING));
				if (!"null".equalsIgnoreCase(sessionNode)) {
					sessionId = sessionNode;
				}
			}

		} catch (final Exception e) {
			// Deliberately not logging the response body: it echoes the
			// SOAP login request on errors and may contain credentials
			this.logger.error("Could not parse SOAP login response", e);
		}

		// When we have non-null instanceHostName and sessionId
		// Our authentication worked
		if ((instanceHostName != null) && (sessionId != null)) {
			result = new AuthInfo(instanceHostName, sessionId);
		}

		return result;
	}

	private Buffer getAuthBody(final String user, final String password) {
		final Map<String, String> values = new HashMap<String, String>();
		values.put("username", user);
		values.put("password", password);
		// TODO: switch to File Callback?
		final InputStream in = this.getClass().getResourceAsStream(Constants.AUTH_SOAP_TEMPLATE);
		if (in == null) {
			this.logger.error("Could not retrieve resource " + Constants.AUTH_SOAP_TEMPLATE);
			return Buffer.buffer();
		}

		final Scanner scanner = new Scanner(in);
		scanner.useDelimiter("\\Z");
		final String authTemplate = scanner.next();
		scanner.close();
		final MustacheFactory mf = new DefaultMustacheFactory();
		final Mustache mustache = mf.compile(new StringReader(authTemplate), "Login");
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final PrintWriter pw = new PrintWriter(out);
		try {
			mustache.execute(pw, values).flush();
			pw.close();
		} catch (final IOException e) {
			this.logger.error("Could not render SOAP login template", e);
			return Buffer.buffer();
		}

		return Buffer.buffer(out.toByteArray());
	}

	private Future<AuthInfo> resultOfAuthentication(final HttpResponse<Buffer> postReturn) {
		// A SOAP fault arrives with a non-2xx status and no sessionId. Reporting
		// the status here keeps it out of the generic "check the log" failure
		final int statusCode = postReturn.statusCode();
		if ((statusCode < 200) || (statusCode >= 300)) {
			return Future.failedFuture("SOAP login failed with HTTP " + statusCode);
		}
		// Get session info
		final Buffer body = postReturn.body();
		final AuthInfo a = this.extractAuthInfoFromBody(body);
		if (a != null) {
			this.setCachedAuthInfo(a);
			return Future.succeededFuture(a);
		}
		return Future.failedFuture("Authentication phase failed, please check log");
	}

}
