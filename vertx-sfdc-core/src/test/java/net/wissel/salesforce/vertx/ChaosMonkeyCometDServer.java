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

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadLocalRandom;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Local mock of the Salesforce CometD endpoints, serving the same
 * paths the CometD listener uses in production. Long-poll responses
 * are scripted through a queue; with a chaosRatio above zero it
 * randomly answers with a 500 instead
 */
public class ChaosMonkeyCometDServer {

	private static final String CLIENT_ID = "monkey1";

	private final Deque<JsonArray> scriptedPollResponses = new ConcurrentLinkedDeque<>();
	private final Vertx vertx;
	private final int chaosRatio;
	private HttpServer server;

	public ChaosMonkeyCometDServer(final Vertx vertx) {
		this(vertx, 0);
	}

	/**
	 * @param vertx the test Vertx instance
	 * @param chaosRatio roughly one in chaosRatio requests fails with
	 *            a 500; zero disables chaos for deterministic tests
	 */
	public ChaosMonkeyCometDServer(final Vertx vertx, final int chaosRatio) {
		this.vertx = vertx;
		this.chaosRatio = chaosRatio;
	}

	public Future<HttpServer> start() {
		return this.vertx.createHttpServer().requestHandler(request -> {
			if (this.chaosStrikes()) {
				request.response().setStatusCode(500).end("Chaos monkey strikes");
				return;
			}
			switch (request.path()) {
				case Constants.URL_HANDSHAKE -> this.handshake(request);
				case Constants.URL_CONNECT -> this.connect(request);
				case Constants.URL_SUBSCRIBE -> this.subscribe(request);
				default -> request.response().setStatusCode(404).end("Unknown path " + request.path());
			}
		}).listen(0).andThen(result -> this.server = result.result());
	}

	public int actualPort() {
		return this.server.actualPort();
	}

	/**
	 * Next long poll answers with the given events plus a successful
	 * connect status, the way Salesforce delivers platform events
	 *
	 * @param events one or more Bayeux event objects
	 */
	public void enqueueEvents(final JsonObject... events) {
		final JsonArray response = new JsonArray();
		for (final JsonObject event : events) {
			response.add(event);
		}
		response.add(new JsonObject().put("channel", "/meta/connect").put("successful", true));
		this.scriptedPollResponses.add(response);
	}

	/**
	 * Next long poll reports an unsuccessful connect, which makes the
	 * listener end its loop - keeps test teardown from hanging
	 */
	public void enqueueEndOfLoop() {
		this.scriptedPollResponses.add(new JsonArray()
				.add(new JsonObject().put("channel", "/meta/connect").put("successful", false)));
	}

	/**
	 * Convenience to build an event the way the listener parses it
	 */
	public static JsonObject event(final String channel, final JsonObject payload, final int replayId) {
		return new JsonObject()
				.put("channel", channel)
				.put("data", new JsonObject()
						.put("payload", payload)
						.put("event", new JsonObject().put("replayId", replayId)));
	}

	private boolean chaosStrikes() {
		return (this.chaosRatio > 0) && (ThreadLocalRandom.current().nextInt(this.chaosRatio) == 0);
	}

	private void handshake(final HttpServerRequest request) {
		final JsonArray response = new JsonArray().add(new JsonObject()
				.put("channel", "/meta/handshake")
				.put("successful", true)
				.put("clientId", CLIENT_ID));
		request.response()
				.putHeader("Content-Type", "application/json;charset=UTF-8")
				.putHeader("Set-Cookie", "BAYEUX_BROWSER=cm-test; Path=/; Secure")
				.end(response.encode());
	}

	private void connect(final HttpServerRequest request) {
		request.body().onSuccess(body -> {
			final JsonObject first = new JsonArray(body).getJsonObject(0);
			if (first.containsKey("advice")) {
				// The advice negotiation round
				final JsonArray response = new JsonArray().add(new JsonObject()
						.put("channel", "/meta/connect")
						.put("successful", true)
						.put("advice", new JsonObject().put("timeout", 110000).put("reconnect", "retry")));
				request.response().putHeader("Content-Type", "application/json;charset=UTF-8")
						.end(response.encode());
			} else {
				// The long poll - scripted answer or an empty one after a
				// short wait; always answers, so shutdowns never hang
				final JsonArray scripted = this.scriptedPollResponses.poll();
				final JsonArray response = (scripted != null)
						? scripted
						: new JsonArray().add(new JsonObject().put("channel", "/meta/connect").put("successful", true));
				final long delay = (scripted != null) ? 50 : 200;
				this.vertx.setTimer(delay, timer -> request.response()
						.putHeader("Content-Type", "application/json;charset=UTF-8")
						.end(response.encode()));
			}
		});
	}

	private void subscribe(final HttpServerRequest request) {
		request.body().onSuccess(body -> {
			final String subscription = new JsonArray(body).getJsonObject(0).getString("subscription");
			final JsonArray response = new JsonArray().add(new JsonObject()
					.put("channel", "/meta/subscribe")
					.put("subscription", subscription)
					.put("successful", true));
			request.response().putHeader("Content-Type", "application/json;charset=UTF-8")
					.end(response.encode());
		});
	}
}
