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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.Response;
import net.wissel.salesforce.vertx.config.DedupConfig;

/**
 * Deduplication backed by a Redis server, so several instances can share
 * one view of what has already been seen.
 *
 * Each message is keyed by the MD5 of its body and stored with SET NX, so
 * the store itself decides whether the key is new, and with an expiry so
 * keys fall out after a day
 *
 * @author swissel
 *
 */
public class RedisDedup extends AbstractSFDCDedupVerticle {

	// Keys live in Redis for one full day 60*60*24h
	private static final String LIFESPAN_SECONDS = "86400";
	private static final int DEFAULT_REDIS_PORT = 6379;

	/**
	 * Creates the dedup verticle; Vert.x instantiates it on deployment
	 */
	public RedisDedup() {
		// Nothing to set up
	}

	/**
	 * The Redis client holding data
	 */
	private RedisAPI redisAPI = null;

	@Override
	protected Future<Void> checkForDuplicate(final JsonObject messageBody) {
		final String key = this.getMd5(messageBody);
		return this.storeKey(key).compose(response ->
				// SET with NX answers null when the key already existed
				(response == null)
						? Future.failedFuture(new DuplicateMessageException(key))
						: Future.succeededFuture());
	}

	/**
	 * Stores the key, only if new (NX), expiring after a day (EX).
	 * Separate method, so tests can substitute the Redis server
	 *
	 * @param key the dedup key
	 * @return the raw Redis response, null for an existing key
	 */
	protected Future<Response> storeKey(final String key) {
		return this.getRedisAPI().set(List.of(key, key, "NX", "EX", RedisDedup.LIFESPAN_SECONDS));
	}

	/**
	 * Get a reasonable key value for the object
	 *
	 * @param j
	 *            the Json Object
	 * @return a MD5 String
	 */
	private String getMd5(final JsonObject j) {
		String result = null;
		try {
			final MessageDigest md = MessageDigest.getInstance("MD5");
			final byte[] bresult = md.digest(j.toBuffer().getBytes());
			result = HexFormat.of().withUpperCase().formatHex(bresult);
		} catch (final NoSuchAlgorithmException e) {
			this.logger.error("MD5 digest unavailable", e);
		}
		return result;
	}

	private RedisAPI getRedisAPI() {
		if (this.redisAPI == null) {
			final Redis client = Redis.createClient(this.getVertx(), this.buildRedisOptions());
			this.redisAPI = RedisAPI.api(client);
		}
		return this.redisAPI;
	}

	/**
	 * Builds the connection options for the configured Redis server.
	 * The password travels as its own option rather than inside the
	 * connection URI: a password containing a URI delimiter such as
	 * "@" or "#" would otherwise change how the string is parsed and
	 * break authentication for an otherwise valid password.
	 * Separate method, so tests can inspect the result
	 *
	 * @return options addressing the configured server
	 */
	protected RedisOptions buildRedisOptions() {
		final DedupConfig ddConfig = this.getDedupConfig();
		final int port = (ddConfig.getPort() > 0) ? ddConfig.getPort() : RedisDedup.DEFAULT_REDIS_PORT;
		final RedisOptions options = new RedisOptions()
				.setConnectionString("redis://" + ddConfig.getServerURL() + ":" + port);
		final String password = ddConfig.getSfdcPassword();
		if ((password != null) && !password.isEmpty()) {
			options.setPassword(password);
		}
		return options;
	}
}
