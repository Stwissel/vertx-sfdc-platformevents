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

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import net.wissel.salesforce.vertx.AbstractSFDCVerticle;
import net.wissel.salesforce.vertx.Constants;
import net.wissel.salesforce.vertx.SFDCVerticle;
import net.wissel.salesforce.vertx.config.AuthConfig;

/**
 * Convenience class to
 * 
 * @author swissel
 *
 */
public abstract class AbstractAuth extends AbstractSFDCVerticle implements SFDCVerticle {

	private AuthConfig authConfig = null;
	private AuthInfo cachedAuthInfo = null;
	private MessageConsumer<String> consumer = null;

	// We try only once
	private boolean loginFailed = false;

	/**
	 * @see net.wissel.salesforce.vertx.AbstractSFDCVerticle#start(io.vertx.core.Future)
	 */
	@Override
	public void start(final Future<Void> startFuture) throws Exception {
		// Authentication needs to listen first
		this.startListening();
		super.start(startFuture);
	}

	protected AuthInfo getCachedAuthInfo() {
		return this.cachedAuthInfo;
	}

	protected abstract void login(Future<AuthInfo> futureAuthinfo);

	protected void setCachedAuthInfo(final AuthInfo cacheInfo) {
		this.cachedAuthInfo = cacheInfo;

	}

	@Override
	public SFDCVerticle startListening() {
		// No need to start this if it is running
		if (this.listening && (this.consumer != null)) {
			return this;
		}
		final EventBus eb = this.getVertx().eventBus();
		eb.registerDefaultCodec(AuthInfo.class, new AuthInfoCodec());
		final String address = Constants.BUS_AUTHREQUEST + this.getAuthConfig().getAuthName();
		this.consumer = eb.consumer(address, handler -> {
			// Check for a reset header, if found reset the cached connection
			// info
			// and get a new login
			final MultiMap headers = handler.headers();
			if (headers.contains(Constants.AUTH_RESET)) {
				this.setCachedAuthInfo(null);
				this.loginFailed = false;
			}
			if (this.getCachedAuthInfo() != null) {
				handler.reply(this.getCachedAuthInfo());
			} else {
				if (this.loginFailed) {
					// No second try
					handler.fail(500, "Previous login attempt failed, login disabled " + this.getClass().getName());
				} else {
					final Future<AuthInfo> futureAuthinfo = Future.future();
					futureAuthinfo.setHandler(authHandler -> {
						if (authHandler.succeeded()) {
							handler.reply(authHandler.result());
						} else {
							this.loginFailed = true;
							this.logger.error(authHandler.cause());
							handler.fail(500, authHandler.cause().getMessage());
						}
					});
					this.login(futureAuthinfo);
				}
			}
		});
		this.logger.info("Start listening:" + this.getClass().getName());
		this.listening = true;
		return this;
	}
	/**
	 * @see net.wissel.salesforce.vertx.AbstractSFDCVerticle#stopListening(io.vertx.core.Future)
	 */
	@Override
	public SFDCVerticle stopListening(final Future<Void> stopListenFuture) {
		this.logger.info("Stop listening:" + this.getClass().getName());
		if (this.consumer != null) {
			this.consumer.unregister(handler -> {
				this.consumer = null;
				this.listening = false;
				this.loginFailed = false;
				stopListenFuture.complete();
			});
		} else {
			this.consumer = null;
			this.listening = false;
			this.loginFailed = false;
			stopListenFuture.complete();
		}
		return this;
	}

	/**
	 * Gets the AuthConfig concrete object from the JSON config
	 * @return
	 */
	protected AuthConfig getAuthConfig() {
		if (this.authConfig == null) {
			this.authConfig = this.config().mapTo(AuthConfig.class);
		}
		return this.authConfig;
	}

}
