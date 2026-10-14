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
package net.wissel.salesforce.vertx.dedup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Response;

/**
 * Dedup logic with an in-memory stand-in for the Redis SET NX call -
 * no Redis server or container required
 */
public class RedisDedupTest {

	/**
	 * Replaces the Redis round trip with a map honoring NX semantics:
	 * null response for a key that already exists
	 */
	private static class FakeRedisDedup extends RedisDedup {
		final Set<String> storedKeys = new HashSet<>();
		final List<String> capturedKeys = new ArrayList<>();

		@Override
		protected Future<Response> storeKey(final String key) {
			this.capturedKeys.add(key);
			if (this.storedKeys.add(key)) {
				// A real SET returns an "OK" response - any non-null works here
				// since the production code only checks for null
				return Future.succeededFuture(io.vertx.redis.client.impl.types.SimpleStringType.OK);
			}
			return Future.succeededFuture(null);
		}
	}

	@Test
	public void firstMessagePassesDuplicateFails() {
		final FakeRedisDedup dedup = new FakeRedisDedup();
		final JsonObject message = new JsonObject().put("payload", "data");

		assertTrue(dedup.checkForDuplicate(message).succeeded());
		assertTrue(dedup.checkForDuplicate(message).failed());
	}

	@Test
	public void differentMessagesPassIndependently() {
		final FakeRedisDedup dedup = new FakeRedisDedup();
		assertTrue(dedup.checkForDuplicate(new JsonObject().put("n", 1)).succeeded());
		assertTrue(dedup.checkForDuplicate(new JsonObject().put("n", 2)).succeeded());
		assertTrue(dedup.checkForDuplicate(new JsonObject().put("n", 1)).failed());
	}

	@Test
	public void keyIsStableUppercaseMd5Hex() {
		final FakeRedisDedup dedup = new FakeRedisDedup();
		final JsonObject message = new JsonObject().put("payload", "stable");

		dedup.checkForDuplicate(message);
		dedup.checkForDuplicate(message.copy());

		assertEquals(2, dedup.capturedKeys.size());
		final String first = dedup.capturedKeys.get(0);
		// Same content must hash to the same key
		assertEquals(first, dedup.capturedKeys.get(1));
		// MD5 as uppercase hex: 32 chars
		assertTrue(first.matches("[0-9A-F]{32}"), "Not an uppercase MD5 hex: " + first);
	}
}
