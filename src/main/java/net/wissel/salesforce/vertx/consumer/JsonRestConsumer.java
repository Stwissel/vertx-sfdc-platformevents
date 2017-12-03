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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import net.wissel.salesforce.vertx.Constants;
import net.wissel.salesforce.vertx.SFDCVerticle;
import net.wissel.salesforce.vertx.auth.AuthInfo;

/**
 * Takes incoming messages and posts them to a REST URL endpoint Uses an auth
 * provider to get Authorization header information Eventually transforms the
 * incoming JSON using a Mustache transformation into something else
 *
 * @author swissel
 *
 */
public class JsonRestConsumer extends AbstractSDFCConsumer implements SFDCConsumer {

	/**
	 * Capture retry operations
	 *
	 * @author swissel
	 *
	 */
	private class PostRetry {
		final public JsonObject body;

		public int retryCount = 0;

		public PostRetry(final JsonObject content) {
			this.body = content;
		}
	}

	// TODO: make retry count and interval configurable
	private final int retryCount = 10;
	private final Queue<PostRetry> retryBuffer = new LinkedList<>();
	private AuthInfo authInfo = null;
	private WebClient client = null;

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
		this.getVertx().setPeriodic(10000L, timer -> {
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
	protected void addRoutes(final Router router) {
		// No routes required for this one
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
	 * @return
	 */
	private String getContentType() {
		return this.getConsumerConfig().getParameter(Constants.CONTENT_HEADER, Constants.CONTENT_TYPE_JSON);
	}

	private String getTemplate() {
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
			this.client = WebClient.create(this.vertx, wco);
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
		final Buffer bodyBuffer = this.transformBody(payload.body);
		this.initWebClient();
		final HttpRequest<Buffer> request = this.initWebPostRequest(this.getConsumerConfig().getUrl());
		request.sendBuffer(bodyBuffer, result -> {
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
		if (payload.retryCount < this.retryCount) {
			payload.retryCount++;
			// TODO: handle failure
			this.retryBuffer.offer(payload);
		} else {
			this.logger.fatal(new Exception("POST failed after all retries:" + payload.body.toString()));
		}

	}

	private Buffer transformBody(final JsonObject body) {
		Buffer result = null;
		if (this.needsTransformation()) {
			// TODO: Check how JsonObject works for transformations here
			final FileSystem fs = this.getVertx().fileSystem();
			final Buffer template = fs.readFileBlocking(this.getTemplate());
			final MustacheFactory mf = new DefaultMustacheFactory();
			final Mustache mustache = mf.compile(template.toString());
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final PrintWriter pw = new PrintWriter(out);
			try {
				mustache.execute(pw, body).flush();
				pw.close();
			} catch (final IOException e) {
				this.logger.error(e);
				return Buffer.buffer();
			}
		} else {
			result = body.toBuffer();
		}
		return result;
	}

}
