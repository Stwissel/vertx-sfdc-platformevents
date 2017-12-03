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
package net.wissel.salesforce.vertx.listener;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import net.wissel.salesforce.vertx.AbstractSFDCVerticle;
import net.wissel.salesforce.vertx.Constants;
import net.wissel.salesforce.vertx.SFDCVerticle;
import net.wissel.salesforce.vertx.Utils;
import net.wissel.salesforce.vertx.auth.AuthInfo;
import net.wissel.salesforce.vertx.config.ListenerConfig;

/**
 * @author swissel
 *
 */
public class CometD extends AbstractSFDCVerticle {

	private ListenerConfig listenerConfig = null;
	private AuthInfo authInfo = null;
	protected WebClient client = null;
	private String clientId = null;
	private int connectCounter = 3;
	private final Map<String, String> cookies = new HashMap<String, String>();

	protected void captureCookies(final List<String> newCookies) {
		if (newCookies != null) {
			for (final String newCookie : newCookies) {
				this.captureCookie(newCookie);
			}
		}
	}

	protected ListenerConfig getListenerConfig() {
		if (this.listenerConfig == null) {
			this.listenerConfig = this.config().mapTo(ListenerConfig.class);
		}
		return this.listenerConfig;
	}

	protected synchronized int getNextCounter() {
		// Resetting the counter for super long running
		// processes to prevent overflow
		if (this.connectCounter == Integer.MAX_VALUE) {
			this.connectCounter = 0;
		}
		return this.connectCounter++;
	}

	/* Signal the results to the eventbus */
	protected void processOneResult(final JsonObject dataChange) {

		final JsonObject data = dataChange.getJsonObject("data");
		final JsonObject payload = data.getJsonObject("payload");
		// We send it off to the eventbus
		final EventBus eb = this.getVertx().eventBus();
		this.getListenerConfig().getEventBusAddresses().forEach(destination -> {
			try {
				eb.publish(destination, payload);
				this.logger.info("Sending to [" + destination + "]:" + payload.toString());
			} catch (final Throwable t) {
				this.logger.error(t.getMessage(), t);
			}
		});
	}

	/**
	 * This is where the data leaves the verticle onto the Bus
	 *
	 * @param receivedData
	 *            JSON data
	 */
	protected void processReceivedData(final JsonArray receivedData) {
		// The last element in the Array is the all over status
		for (int i = 0; i < (receivedData.size() - 1); i++) {
			this.processOneResult(receivedData.getJsonObject(i));
		}

	}

	@Override
	public SFDCVerticle startListening() {

		// First get a proper session
		this.getAuthInfo().setHandler(handler -> {
			if (handler.succeeded()) {
				// We got a session
				this.authInfo = handler.result();
				// This is where we talk to the API
				this.step2ActionHandshake();
			} else {
				this.logger.fatal(handler.cause());
			}
		});
		this.listening = true;
		return this;
	}

	@Override
	public SFDCVerticle stopListening(final Future<Void> stopListenFuture) {
		this.shuttingDown = true;
		if (this.shutdownCompleted) {
			this.authInfo = null;
			this.listening = false;
			stopListenFuture.complete();
		} else {
			// We need to wait for the next cycle
			// There might be an incoming request
			// We don't want to miss
			this.getVertx().setTimer(1000, en -> {
				try {
					this.stopListening(stopListenFuture);
				} catch (final Exception e) {
					this.logger.error(e);
				}
			});
			
		}
		return this;
	}

	private void captureCookie(final String newCookie) {
		final String cookieName = newCookie.substring(0, newCookie.indexOf("="));
		this.cookies.put(cookieName, newCookie);
	}

	private JsonArray getAdviceBody() {
		final JsonArray result = new JsonArray();
		final JsonObject advBody = new JsonObject();
		advBody.put("clientId", this.clientId);
		advBody.put("advice", new JsonObject("{\"timeout\": 0}"));
		advBody.put("channel", "/meta/connect");
		advBody.put("id", "2");
		advBody.put("connectionType", "long-polling");
		result.add(advBody);
		return result;
	}

	private Future<AuthInfo> getAuthInfo() {
		Future<AuthInfo> result;
		if (this.authInfo == null) {
			result = Future.future();
			final EventBus eb = this.getVertx().eventBus();
			final String address = Constants.BUS_AUTHREQUEST + this.getListenerConfig().getAuthName();
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

	private JsonArray getConnectBody() {
		final JsonArray result = new JsonArray();
		final JsonObject connect = new JsonObject();
		connect.put("clientId", this.clientId);
		connect.put("channel", "/meta/connect");
		connect.put("id", String.valueOf(this.getNextCounter()));
		connect.put("connectionType", "long-polling");
		result.add(connect);
		return result;
	}

	/**
	 * Initial handshake hardcoded;
	 *
	 * @return
	 */
	private JsonArray getHandshakeBody() {
		final JsonArray result = new JsonArray();
		final JsonObject handshake = new JsonObject();
		handshake.put("ext", new JsonObject("{\"replay\" : true}"));
		handshake.put("supportedConnectionTypes", new JsonArray().add("long-polling"));
		handshake.put("channel", "/meta/handshake");
		handshake.put("id", "1");
		handshake.put("version", "1.0");
		result.add(handshake);
		return result;
	}

	private JsonArray getSubscriptionBody() {
		final JsonArray result = new JsonArray();
		final JsonObject subscribe = new JsonObject();
		final JsonObject ext = new JsonObject();
		final JsonObject replay = new JsonObject();
		// TODO: Handling of Replay options need to be fixed here!
		replay.put(this.getListenerConfig().getListenSubject(), -2);
		ext.put("replay", replay);
		subscribe.put("ext", ext);
		subscribe.put("clientId", this.clientId);
		subscribe.put("channel", "/meta/subscribe");
		subscribe.put("subscription", this.getListenerConfig().getListenSubject());
		subscribe.put("id", "3");
		result.add(subscribe);
		return result;
	}

	private WebClient initWebClient() {
		if (this.client == null) {
			final WebClientOptions wco = new WebClientOptions();
			final String proxyHost = this.getListenerConfig().getProxy();
			final int proxyPort = this.getListenerConfig().getProxyPort();
			if ((proxyHost != null) && (proxyPort > 0)) {
				final ProxyOptions po = new ProxyOptions();
				wco.setProxyOptions(po);
				po.setHost(proxyHost).setPort(proxyPort);
			}
			// TODO: more options
			wco.setUserAgent("SDFC VertX EventBus Client");
			wco.setTryUseCompression(true);
			this.client = WebClient.create(this.vertx, wco);
		}
		return this.client;
	}

	private HttpRequest<Buffer> initWebPostRequest(final String destination) {
		final WebClient client = this.initWebClient();
		final HttpRequest<Buffer> request = client.post(Constants.TLS_PORT, this.authInfo.serverName, destination)
				.ssl(true).putHeader("Authorization", this.authInfo.sessionToken)
				.putHeader("Content-Type", "application/json;charset=UTF-8");
		final MultiMap headers = request.headers();
		headers.add("Cookie", this.cookies.values());
		return request;
	}

	private void step2ActionHandshake() {
		if (this.shuttingDown || this.shutdownCompleted) {
			this.shutdownCompleted = true;
			return;
		}
			final HttpRequest<Buffer> request = this.initWebPostRequest(Constants.URL_HANDSHAKE);
			final JsonArray body = this.getHandshakeBody();

			request.sendJson(body, postReturn -> {
				if (postReturn.succeeded()) {
					this.step2ResultHandshake(postReturn.result());
				} else {
					this.logger.error(postReturn.cause());
				}
			});
	}

	private void step2ResultHandshake(final HttpResponse<Buffer> postReturn) {
		// Don't proceed in a shutdown scenario
		if (this.shuttingDown || this.shutdownCompleted) {
			this.shutdownCompleted = true;
			return;
		}

		// Handle cookies
		this.captureCookies(postReturn.cookies());

		// Handle the body
		final JsonObject handshakeResult = postReturn.bodyAsJsonArray().getJsonObject(0);
		// Check if it worked
		if (handshakeResult.getBoolean("successful", false)) {
			final String clientIdCandidate = handshakeResult.getString("clientId");
			if (clientIdCandidate == null) {
				this.logger.error("Handshake didn't provide clientId");
			} else {
				this.clientId = clientIdCandidate;
				this.step3ActionAdvice(handshakeResult);
			}
		} else {
			this.logger.error("Handshake was unsuccessful");
		}
	}

	private void step3ActionAdvice(final JsonObject handshakeResult) {
		if (this.shuttingDown || this.shutdownCompleted) {
			this.shutdownCompleted = true;
			return;
		}

		final JsonArray body = this.getAdviceBody();
		final HttpRequest<Buffer> request = this.initWebPostRequest(Constants.URL_CONNECT);
		request.sendJson(body, postReturn -> {
			if (postReturn.succeeded()) {
				this.step3ResultAdvice(postReturn.result());
			} else {
				this.logger.error(postReturn.cause());
			}
		});
	}

	private void step3ResultAdvice(final HttpResponse<Buffer> postReturn) {
		if (this.shuttingDown || this.shutdownCompleted) {
			this.shutdownCompleted = true;
			return;
		}

		// Handle cookies
		this.captureCookies(postReturn.cookies());
		// Handle body
		final JsonObject advice = postReturn.bodyAsJsonArray().getJsonObject(0);
		if (advice.getBoolean("successful", false)) {
			// TODO Extract the advice, especially timeout
			this.step4ActionSubscribe();
		} else {
			this.logger.error("Advice negotiation failed (Step 3)");
		}
	}

	private void step4ActionSubscribe() {
		if (this.shuttingDown || this.shutdownCompleted) {
			this.shutdownCompleted = true;
			return;
		}

		final JsonArray body = this.getSubscriptionBody();
		final HttpRequest<Buffer> request = this.initWebPostRequest(Constants.URL_SUBSCRIBE);

		request.sendJson(body, postReturn -> {
			if (postReturn.succeeded()) {
				this.step4ResultSubscribe(postReturn.result());
			} else {
				this.logger.error(postReturn.cause());
			}
		});
	}

	private void step4ResultSubscribe(final HttpResponse<Buffer> postReturn) {
		if (this.shuttingDown || this.shutdownCompleted) {
			this.shutdownCompleted = true;
			return;
		}
		this.captureCookies(postReturn.cookies());
		// Now we can enter the loop that goes on and on
		this.subscriptionFetch();

	}

	private void subscriptionFetch() {
		if (this.shuttingDown || this.shutdownCompleted) {
			this.shutdownCompleted = true;
			return;
		}
		this.logger.info("Fetch "+ Utils.getDateString(new Date()));
		final JsonArray body = this.getConnectBody();
		final HttpRequest<Buffer> request = this.initWebPostRequest(Constants.URL_CONNECT);
		request.as(BodyCodec.jsonArray()).sendJson(body, this::subscriptionResult);

	}

	private void subscriptionResult(final AsyncResult<HttpResponse<JsonArray>> ar) {
		if (ar.succeeded()) {
			// Process the result
			this.captureCookies(ar.result().cookies());
			final JsonArray receivedData = ar.result().body();
			final JsonObject status = receivedData.getJsonObject(receivedData.size() - 1);
			if (status.getBoolean("successful", false)) {
				// If the array has only one member we didn't get new data
				if (receivedData.size() > 1) {
					this.processReceivedData(receivedData);
				}
				// Do it again eventually
				if (!this.shuttingDown && !this.shutdownCompleted) {
					this.subscriptionFetch();
				} else {
					this.shutdownCompleted = true;
				}
			} else {
				// We won't continue
				this.logger.fatal(status.encodePrettily());
				this.shutdownCompleted = true;
			}

		} else {
			// Preliminary stopping here
			// needs to be handled
			this.logger.fatal(ar.cause());
			this.shutdownCompleted = true;
		}
	}

}
