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

	// The login currently in flight, shared by every waiting request
	private Future<AuthInfo> pendingLogin = null;

	/**
	 * Creates the shared auth base for a subclass
	 */
	protected AbstractAuth() {
		// Nothing to set up
	}

	/**
	 * @see net.wissel.salesforce.vertx.AbstractSFDCVerticle#start()
	 */
	@Override
	public Future<?> start() throws Exception {
		// Authentication needs to listen first
		this.startListening();
		return super.start();
	}

	/**
	 * Gets the session currently cached, if a login has already succeeded
	 *
	 * @return the cached session, or null when none is held
	 */
	protected AuthInfo getCachedAuthInfo() {
		return this.cachedAuthInfo;
	}

	/**
	 * Performs the actual login against Salesforce.
	 *
	 * Implementations are expected to cache the result via
	 * {@link #setCachedAuthInfo(AuthInfo)} so later requests are served from
	 * the cache instead of logging in again
	 *
	 * @return a future carrying the session, failing when the login is refused
	 */
	protected abstract Future<AuthInfo> login();

	/**
	 * Stores the session to serve later requests from
	 *
	 * @param cacheInfo the session to cache, or null to drop what is held
	 */
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
		final String address = Constants.BUS_AUTHREQUEST + this.getAuthConfig().getAuthName();
		this.consumer = eb.consumer(address, handler -> {
			// Check for a reset header, if found reset the cached connection
			// info
			// and get a new login
			final MultiMap headers = handler.headers();
			if (headers.contains(Constants.AUTH_RESET)) {
				this.setCachedAuthInfo(null);
				this.loginFailed = false;
				// A reset asks for a fresh login, so an in-flight one started
				// before the reset must not be handed out any more
				this.pendingLogin = null;
			}
			if (this.getCachedAuthInfo() != null) {
				handler.reply(this.getCachedAuthInfo());
			} else {
				if (this.loginFailed) {
					// No second try
					handler.fail(500, "Previous login attempt failed, login disabled " + this.getClass().getName());
				} else {
					// One login serves every request that arrives while it is
					// in flight. Starting one per caller means one SOAP session
					// per caller with SoapApi. A verticle runs on a single
					// event loop thread, so this needs no synchronization
					Future<AuthInfo> inFlight = this.pendingLogin;
					if (inFlight == null) {
						inFlight = this.login();
						// Publish the field before registering the reset
						// handler: a login that is already complete runs that
						// handler at once, and only the local reference is
						// guaranteed to still point at the future afterwards
						this.pendingLogin = inFlight;
						inFlight.onComplete(result -> this.pendingLogin = null);
					}
					inFlight.onComplete(authHandler -> {
						if (authHandler.succeeded()) {
							handler.reply(authHandler.result());
						} else {
							this.loginFailed = true;
							this.logger.error("Login failed", authHandler.cause());
							handler.fail(500, authHandler.cause().getMessage());
						}
					});
				}
			}
		});
		this.logger.info("Start listening: {}", this.getClass().getName());
		this.listening = true;
		return this;
	}
	/**
	 * @see net.wissel.salesforce.vertx.AbstractSFDCVerticle#stopListening()
	 */
	@Override
	public Future<Void> stopListening() {
		this.logger.info("Stop listening: {}", this.getClass().getName());
		final Future<Void> unregister = (this.consumer != null)
				? this.consumer.unregister()
				: Future.succeededFuture();
		return unregister.andThen(handler -> {
			this.consumer = null;
			this.listening = false;
			this.loginFailed = false;
			this.pendingLogin = null;
		});
	}

	/**
	 * Gets the AuthConfig concrete object from the JSON config
	 * @return the auth config
	 */
	protected AuthConfig getAuthConfig() {
		if (this.authConfig == null) {
			this.authConfig = this.config().mapTo(AuthConfig.class);
		}
		return this.authConfig;
	}

}
