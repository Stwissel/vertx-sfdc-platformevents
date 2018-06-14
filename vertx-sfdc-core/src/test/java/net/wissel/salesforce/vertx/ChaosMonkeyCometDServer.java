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

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * Module to allow to test the CometD listener Reacts to all steps the cometD
 * server does with good or bad results. Generates semi random content
 * 
 * @author stw
 *
 */
public class ChaosMonkeyCometDServer extends AbstractVerticle {

	public static final int chaosMonkeyPort = 4561;

	// Ratio 1:chaosRatio the server will throw
	// errors instead of returning correct results
	// 0 = no errors, 1 = 50%, 10 = 10% ect.
	private int chaosRatio = 10;
	private Router router;
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	/**
	 * @see io.vertx.core.AbstractVerticle#start(io.vertx.core.Future)
	 */
	@Override
	public void start(Future<Void> startFuture) throws Exception {

		// Routes we server simulating the login and CometD endpoints
		this.router = Router.router(this.getVertx());
		this.router.route(Constants.AUTH_SOAP_LOGIN).handler(this::loginHandler);
		this.router.route(Constants.URL_CONNECT).handler(this::connectHandler);
		this.router.route(Constants.URL_HANDSHAKE).handler(this::handshakeHandler);
		this.router.route(Constants.URL_SUBSCRIBE).handler(this::subscribeHandler);
		this.logger.info(this.getClass().getName() + " listening on port " + chaosMonkeyPort);
		this.getVertx().createHttpServer().requestHandler(this.router::accept).listen(chaosMonkeyPort);
		startFuture.complete();
	}

	private void loginHandler(final RoutingContext ctx) {
		if (this.sendValidResponse()) {
			//TODO: implement
			ctx.response().end("loginHandler");
		} else {
			ctx.response().setStatusCode(500).end("Chaos Monkey says hello!");
		}
	}

	private void connectHandler(final RoutingContext ctx) {
		if (this.sendValidResponse()) {
			//TODO: implement
			ctx.response().end("connectHandler");
		} else {
			ctx.response().setStatusCode(500).end("Chaos Monkey says hello!");
		}
	}

	private void handshakeHandler(final RoutingContext ctx) {
		if (this.sendValidResponse()) {
			//TODO: implement
			ctx.response().end("handshakeHandler");
		} else {
			ctx.response().setStatusCode(500).end("Chaos Monkey says hello!");
		}
	}

	private void subscribeHandler(final RoutingContext ctx) {
		if (this.sendValidResponse()) {
			//TODO: implement
			ctx.response().end("subscribeHandler");
		} else {
			ctx.response().setStatusCode(500).end("Chaos Monkey says hello!");
		}
	}

	/**
	 * Chaos monkey function that decides if a request will get a proper return
	 * or throw an error
	 * 
	 * @return shall the reply be valid
	 */
	private boolean sendValidResponse() {

		// If chaosRatio is 0 we always send valid responses
		if (this.chaosRatio <= 0) {
			return true;
		}

		List<Boolean> result = new ArrayList<>();
		for (int i = 0; i < this.chaosRatio; i++) {
			result.add(true); // All good responses
		}
		result.add(false); // One Error

		int judge = (int) Math.random() * this.chaosRatio;

		return result.get(judge);
	}

}
