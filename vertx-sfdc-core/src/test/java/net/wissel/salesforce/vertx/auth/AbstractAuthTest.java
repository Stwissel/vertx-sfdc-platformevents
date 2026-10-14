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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.wissel.salesforce.vertx.Constants;
import net.wissel.salesforce.vertx.TestUtilities;
import net.wissel.salesforce.vertx.config.AuthConfig;

/**
 * Caching, failure and reset behavior shared by all auth verticles
 */
@ExtendWith(VertxExtension.class)
public class AbstractAuthTest {

	private static final String ADDRESS = Constants.BUS_AUTHREQUEST + "counting";

	/**
	 * Test double that counts login attempts and can be told to fail
	 */
	public static class CountingAuth extends AbstractAuth {
		final AtomicInteger loginCount = new AtomicInteger(0);
		volatile boolean failLogins = false;
		// When above zero the login resolves on a timer rather than at once,
		// which is what leaves a window for concurrent requests to share it
		volatile long loginDelayMs = 0;

		@Override
		protected Future<AuthInfo> login() {
			this.loginCount.incrementAndGet();
			if (this.failLogins) {
				return Future.failedFuture("Login refused by test");
			}
			final AuthInfo ai = new AuthInfo("counting.example.com", "token-" + this.loginCount.get());
			if (this.loginDelayMs > 0) {
				final Promise<AuthInfo> delayed = Promise.promise();
				this.getVertx().setTimer(this.loginDelayMs, timer -> {
					this.setCachedAuthInfo(ai);
					delayed.complete(ai);
				});
				return delayed.future();
			}
			this.setCachedAuthInfo(ai);
			return Future.succeededFuture(ai);
		}
	}

	private Future<String> deploy(final Vertx vertx, final CountingAuth auth) {
		TestUtilities.registerAuthCodec(vertx);
		final AuthConfig config = new AuthConfig();
		config.setAuthName("counting");
		config.setServerURL("counting.example.com");
		return vertx.deployVerticle(auth, new DeploymentOptions().setConfig(JsonObject.mapFrom(config)));
	}

	@Test
	public void secondRequestIsServedFromCache(final Vertx vertx, final VertxTestContext testContext) {
		final CountingAuth auth = new CountingAuth();
		this.deploy(vertx, auth)
				.compose(id -> vertx.eventBus().<AuthInfo>request(ADDRESS, null))
				.compose(first -> vertx.eventBus().<AuthInfo>request(ADDRESS, null))
				.onComplete(testContext.succeeding(second -> testContext.verify(() -> {
					assertEquals(1, auth.loginCount.get());
					assertEquals("token-1", second.body().sessionToken);
					testContext.completeNow();
				})));
	}

	@Test
	public void resetHeaderForcesNewLogin(final Vertx vertx, final VertxTestContext testContext) {
		final CountingAuth auth = new CountingAuth();
		final DeliveryOptions resetOptions = new DeliveryOptions().addHeader(Constants.AUTH_RESET, Constants.TRUESTRING);
		this.deploy(vertx, auth)
				.compose(id -> vertx.eventBus().<AuthInfo>request(ADDRESS, null))
				.compose(first -> vertx.eventBus().<AuthInfo>request(ADDRESS, null, resetOptions))
				.onComplete(testContext.succeeding(second -> testContext.verify(() -> {
					assertEquals(2, auth.loginCount.get());
					assertEquals("token-2", second.body().sessionToken);
					testContext.completeNow();
				})));
	}

	@Test
	public void failedLoginDisablesRetriesUntilReset(final Vertx vertx, final VertxTestContext testContext) {
		final CountingAuth auth = new CountingAuth();
		auth.failLogins = true;
		this.deploy(vertx, auth)
				.compose(id -> vertx.eventBus().<AuthInfo>request(ADDRESS, null))
				.transform((result, firstFailure) -> {
					testContext.verify(() -> {
						assertTrue(firstFailure instanceof ReplyException);
						assertEquals(500, ((ReplyException) firstFailure).failureCode());
						assertEquals(1, auth.loginCount.get());
					});
					// Second request must fail fast without another login attempt
					return vertx.eventBus().<AuthInfo>request(ADDRESS, null);
				})
				.transform((result, secondFailure) -> {
					testContext.verify(() -> {
						assertEquals(1, auth.loginCount.get());
						assertTrue(String.valueOf(((ReplyException) secondFailure).getMessage())
								.contains("Previous login attempt failed"));
					});
					// A reset re-enables logging in
					auth.failLogins = false;
					final DeliveryOptions resetOptions = new DeliveryOptions().addHeader(Constants.AUTH_RESET,
							Constants.TRUESTRING);
					return vertx.eventBus().<AuthInfo>request(ADDRESS, null, resetOptions);
				})
				.onComplete(testContext.succeeding(reply -> testContext.verify(() -> {
					assertEquals(2, auth.loginCount.get());
					assertEquals("token-2", reply.body().sessionToken);
					testContext.completeNow();
				})));
	}

	@Test
	public void concurrentRequestsShareOneLogin(final Vertx vertx, final VertxTestContext testContext) {
		final CountingAuth auth = new CountingAuth();
		// The cache is only filled once the login resolves, so all five
		// requests below arrive while it is still pending
		auth.loginDelayMs = 200;
		this.deploy(vertx, auth)
				.compose(id -> {
					final List<Future<?>> waiting = new ArrayList<>();
					for (int i = 0; i < 5; i++) {
						waiting.add(vertx.eventBus().request(ADDRESS, null));
					}
					return Future.all(waiting);
				})
				.onComplete(testContext.succeeding(composite -> testContext.verify(() -> {
					// All five served, but the login happened exactly once
					assertEquals(5, composite.size());
					assertEquals(1, auth.loginCount.get());
					testContext.completeNow();
				})));
	}

	@Test
	public void undeployedAuthNoLongerAnswers(final Vertx vertx, final VertxTestContext testContext) {
		final CountingAuth auth = new CountingAuth();
		this.deploy(vertx, auth)
				.compose(id -> vertx.eventBus().<AuthInfo>request(ADDRESS, null).map(reply -> id))
				.compose(vertx::undeploy)
				.compose(v -> vertx.eventBus().<AuthInfo>request(ADDRESS, null))
				.onComplete(testContext.failing(t -> testContext.completeNow()));
	}
}
