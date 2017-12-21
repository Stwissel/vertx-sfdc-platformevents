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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

/**
 * @author swissel
 *
 */
public class RedisDedup extends AbstractSFDCDedupVerticle {

	// EX 86400 = Storing data in Redis for one full day 60*60*24h
	// NX = Create only if not exist
	private final static String DEFAULT_LIFESPAN = " NX EX 86400";

	/**
	 * The Redis client holding data
	 */
	private RedisClient redisClient = null;

	@Override
	protected void checkForDuplicate(final Future<Void> failIfDuplicate, final JsonObject messageBody) {
		final String key = this.getMd5(messageBody);
		this.getRedisClient().set(key + RedisDedup.DEFAULT_LIFESPAN, key, result -> {
			if (result.succeeded()) {
				failIfDuplicate.complete();
			} else {
				failIfDuplicate.fail("Message already processes " + key);
			}
		});
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
			result = DatatypeConverter.printHexBinary(bresult);
		} catch (final NoSuchAlgorithmException e) {
			this.logger.fatal(e);
		}
		return result;
	}

	private RedisClient getRedisClient() {
		if (this.redisClient == null) {
			// TODO: Read redisClient configuration from condfig
			final RedisOptions config = new RedisOptions().setHost("127.0.0.1");
			this.redisClient = RedisClient.create(this.getVertx(), config);
		}
		return this.redisClient;
	}
}
