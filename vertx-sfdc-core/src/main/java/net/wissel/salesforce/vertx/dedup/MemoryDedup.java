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

import java.util.LinkedList;
import java.util.Queue;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * Checks the last 100 messages in memory to be exact duplicates of
 * each other
 * @author swissel
 *
 */
public class MemoryDedup extends AbstractSFDCDedupVerticle {
	
	private final Queue<String> memoryQueue = new LinkedList<String>();
	private static final int MAX_MEMBERS = 100;

	/**
	 * Actual routine that check for "duplication". Could be anything, depending on use case.
	 * The future fails when a duplicate is found and succeeds when it is not.
	 * This allows for async execution
	 * 
	 * @see net.wissel.salesforce.vertx.dedup.AbstractSFDCDedupVerticle#checkForDuplicate(io.vertx.core.Future, io.vertx.core.json.JsonObject)
	 */
	@Override
	protected void checkForDuplicate(final Future<Void> failIfDuplicate, final JsonObject messageBody) {
		final String candidate = messageBody.encode();
		if (this.memoryQueue.contains(candidate)) {
			// We have a duplicate and fail the future
			failIfDuplicate.fail("Duplicate");
		} else {
			this.memoryQueue.offer(candidate);
			// Limit the size of the queue
			while (this.memoryQueue.size() > MAX_MEMBERS) {
				this.memoryQueue.poll();
			}
			failIfDuplicate.complete();
		}

	}

}
