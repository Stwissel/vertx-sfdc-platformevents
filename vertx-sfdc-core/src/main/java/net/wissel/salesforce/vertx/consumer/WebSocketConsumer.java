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

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import net.wissel.salesforce.vertx.SFDCRouterExtension;

/**
 * @author swissel
 *
 */
public class WebSocketConsumer extends AbstractSFDCConsumer implements SFDCRouterExtension {
	
	@Override
	public SFDCRouterExtension addRoutes(final Router router) {
		// Socket handler
		final SockJSHandlerOptions options = new SockJSHandlerOptions().setHeartbeatInterval(2000);
		final SockJSHandler sockJSHandler = SockJSHandler.create(this.vertx, options);

		final BridgeOptions bo = new BridgeOptions()
				.addOutboundPermitted(new PermittedOptions().setAddress(this.getWebSocketName()));

		sockJSHandler.bridge(bo, event -> {
			this.logger.info("A websocket event occurred: " + event.type() + "; " + event.getRawMessage());
			event.complete(true);
		});

		final String someURL = this.getConsumerConfig().getUrl();
		this.logger.info("Router listening on " + someURL + " for " + this.getWebSocketName());
		router.route(someURL).handler(sockJSHandler);
		return this;
	}

	@Override
	protected void processIncoming(final Message<JsonObject> incomingData) {
		final JsonObject body = incomingData.body();
		this.logger.info("Published to eventbus:" + body.toString());
		this.getVertx().eventBus().publish(this.getWebSocketName(), body);
	}

	private String getWebSocketName() {
		return this.getConsumerConfig().getParameters().get("websocketname");
	}

}
