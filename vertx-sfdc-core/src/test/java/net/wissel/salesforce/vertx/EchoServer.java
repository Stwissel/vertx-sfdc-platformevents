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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

/**
 * Local HTTP endpoint for consumer tests. Records what it receives
 * and can be scripted to fail the next n requests with a 500
 */
public class EchoServer {

	/**
	 * One recorded request
	 */
	public record ReceivedRequest(String method, String path, MultiMap headers, String body) {
	}

	private final List<ReceivedRequest> received = new CopyOnWriteArrayList<>();
	private final AtomicInteger failuresLeft = new AtomicInteger(0);
	private HttpServer server;

	/**
	 * Starts the echo server on a random free port
	 *
	 * @param vertx the test Vertx instance
	 * @return Future with the started server (port via actualPort())
	 */
	public Future<HttpServer> start(final Vertx vertx) {
		return vertx.createHttpServer().requestHandler(request -> request.body().onSuccess(body -> {
			this.received.add(new ReceivedRequest(request.method().name(), request.path(),
					request.headers(), body.toString()));
			if (this.failuresLeft.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
				request.response().setStatusCode(500).end("Scripted failure");
			} else {
				request.response().setStatusCode(200).end("OK");
			}
		})).listen(0).andThen(result -> this.server = result.result());
	}

	public int actualPort() {
		return this.server.actualPort();
	}

	public void failNextRequests(final int count) {
		this.failuresLeft.set(count);
	}

	public List<ReceivedRequest> received() {
		return this.received;
	}
}
