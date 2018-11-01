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
package net.wissel.salesforce.vertx.consumer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Queue;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import net.wissel.salesforce.vertx.Constants;
import net.wissel.salesforce.vertx.SFDCVerticle;
import net.wissel.salesforce.vertx.Utils;
import net.wissel.salesforce.vertx.auth.AuthInfo;

/**
 * Takes incoming messages and posts them to a REST URL endpoint Uses an auth
 * provider to get Authorization header information Eventually transforms the
 * incoming JSON using a Mustache transformation into something else
 *
 * @author swissel
 *
 */
public class RestConsumer extends AbstractSFDCConsumer {

	/**
	 * Capture retry operations
	 *
	 * @author swissel
	 *
	 */
	private class PostRetry {
		final public Buffer body;
		public int retryCount = 0;

		public PostRetry(final JsonObject content) {
			this.body = RestConsumer.this.transformBody(content);
		}
	}

	private final Queue<PostRetry> retryBuffer = new LinkedList<>();
	private AuthInfo authInfo = null;
	private WebClient client = null;
	private Mustache mustache = null;

	@Override
	public SFDCVerticle startListening() {

		// First get a proper session
		this.getAuthInfo().setHandler(handler -> {
			if (handler.succeeded()) {
				// We eventually got a session
				this.authInfo = handler.result();
			} else {
				this.logger.fatal(handler.cause());
			}
		});

		// Retry timer - every 10 seconds
		this.getVertx().setPeriodic(this.getInterValtime(), timer -> {
			while (!this.retryBuffer.isEmpty()) {
				final PostRetry payload = this.retryBuffer.poll();
				if (payload == null) {
					break;
				}
				this.postToDestination(payload);
			}
		});

		// Do all the other stuff
		return super.startListening();
	}

	@Override
	protected void processIncoming(final Message<JsonObject> incomingData) {
		final JsonObject body = incomingData.body();
		final PostRetry payload = new PostRetry(body);
		this.postToDestination(payload);
	}

	/**
	 * Check for the Authentication info if required
	 *
	 * @return a future that resolves when successful got AuthInfo
	 */
	private Future<AuthInfo> getAuthInfo() {
		Future<AuthInfo> result;
		final String authName = this.getConsumerConfig().getAuthName();
		if ((this.authInfo == null) && (authName != null)) {
			result = Future.future();
			final EventBus eb = this.getVertx().eventBus();
			final String address = Constants.BUS_AUTHREQUEST + authName;
			eb.send(address, null, replyHandler -> {
				if (replyHandler.succeeded()) {
					this.authInfo = (AuthInfo) replyHandler.result().body();
					result.complete(this.authInfo);
				} else {
					result.fail(replyHandler.cause());
				}
			});

		} else {
			result = Future.succeededFuture(this.authInfo);
		}
		return result;
	}

	/**
	 * Returns the content type
	 *
	 * @return The content type, defaults to JSON
	 */
	private String getContentType() {
		return this.getConsumerConfig().getParameter(Constants.CONTENT_HEADER, Constants.CONTENT_TYPE_JSON);
	}

	/**
	 * How long is the interval between retries Retrieved from parameters,
	 * default 10 sec
	 * 
	 * @return the interval
	 */
	private long getInterValtime() {
		final String intervalCandidate = this.getConsumerConfig().getParameter("interval", "10000");
		return Long.valueOf(intervalCandidate);
	}

	private int getMaxRetryCount() {
		final String retryCandidate = this.getConsumerConfig().getParameter("maxRetryCount", "10");
		return Integer.valueOf(retryCandidate);
	}

	/**
	 * @return a Mustache template compiled
	 */
	private Mustache getMustache() {
		if (this.mustache == null) {
			final FileSystem fs = this.getVertx().fileSystem();
			final Buffer templateBuffer = fs.readFileBlocking(this.getTemplateName());
			final MustacheFactory mf = new DefaultMustacheFactory();
			final ByteArrayInputStream bi = new ByteArrayInputStream(templateBuffer.getBytes());
			this.mustache = mf.compile(new InputStreamReader(bi), "Transform");
		}
		return this.mustache;
	}

	private String getTemplateName() {
		return this.getConsumerConfig().getParameter(Constants.PARAM_STYLESHEET);
	}

	private WebClient initWebClient() {
		if (this.client == null) {
			final WebClientOptions wco = new WebClientOptions();
			final String proxyHost = this.getConsumerConfig().getProxy();
			final int proxyPort = this.getConsumerConfig().getProxyPort();
			if ((proxyHost != null) && (proxyPort > 0)) {
				final ProxyOptions po = new ProxyOptions();
				wco.setProxyOptions(po);
				po.setHost(proxyHost).setPort(proxyPort);
			}
			// TODO: more options
			wco.setUserAgent("SFDC VertX REST Provider");
			wco.setTryUseCompression(true);
			this.client = WebClient.create(this.getVertx(), wco);
		}
		return this.client;
	}

	private HttpRequest<Buffer> initWebPostRequest(final String destination) {
		final WebClient client = this.initWebClient();
		final HttpRequest<Buffer> request = client.post(destination).ssl(true)
				.putHeader(Constants.AUTH_HEADER, this.authInfo.sessionToken)
				.putHeader(Constants.CONTENT_HEADER, this.getContentType());
		return request;
	}

	private boolean needsTransformation() {
		return this.getConsumerConfig().hasParameter(Constants.PARAM_STYLESHEET);
	}

	private void postToDestination(final PostRetry payload) {
		this.initWebClient();
		final HttpRequest<Buffer> request = this.initWebPostRequest(this.getConsumerConfig().getUrl());
		request.sendBuffer(payload.body, result -> {
			// Retry if it didn't work
			if (result.failed()) {
				this.logger.error(result.cause());
				this.processError(payload);
			} else {
				final HttpResponse<Buffer> response = result.result();
				// Check for return code
				if ((response.statusCode() >= 200) && (response.statusCode() < 300)) {
					// We are good
					this.logger.info("Successful post:" + payload.body.toString());
				} else {
					this.processError(payload);
				}
			}
		});
	}

	private void processError(final PostRetry payload) {
		if (payload.retryCount < this.getMaxRetryCount()) {
			payload.retryCount++;
			// TODO: handle failure
			this.retryBuffer.offer(payload);
		} else {
			this.logger.fatal(new Exception("POST failed after all retries:" + payload.body.toString()));
		}

	}

	/**
	 * Turns the JsonObject that came over the wire into a buffer object to be
	 * used in the HTTP Post. Special twist: if configured the JSONObject is run
	 * through a {{Mustache}} transformation, so the result can be anything
	 * JSON, HTML, XML, PlainText, WebForm etc. Allows ultimate flexibility when
	 * one knows Mustache
	 *
	 * @param Json
	 *            Object with incoming payload
	 * @return a Buffer object to be pasted
	 */
	private Buffer transformBody(final JsonObject body) {
		Buffer result = null;
		if (this.needsTransformation()) {
			final Mustache mustache = this.getMustache();
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final PrintWriter pw = new PrintWriter(out);
			try {
				mustache.execute(pw, Utils.mappifyJsonObject(body)).flush();
				pw.close();
				result = Buffer.buffer(out.toByteArray());
			} catch (final IOException e) {
				this.logger.error(e);
				// Get back the unchanged body
				result = body.toBuffer();
			}
		} else {
			result = body.toBuffer();
		}
		return result;
	}

}
