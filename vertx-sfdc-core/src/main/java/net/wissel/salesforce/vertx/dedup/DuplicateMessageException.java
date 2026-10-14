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

/**
 * Marks a message that the dedup store has seen before.
 *
 * A dedup check reports its outcome by failing the returned future, which on
 * its own cannot tell "this is a duplicate" apart from "the dedup store was
 * unreachable". The first is routine and the message is dropped; the second
 * is an incident and the message must not be silently lost. Failing with this
 * type makes a confirmed duplicate recognizable, so any other cause can be
 * treated as a storage failure.
 *
 * It is used as a future's failure cause and never actually thrown, so it
 * carries no stack trace - duplicates are a steady-state occurrence and
 * capturing a trace for each one would be wasted work.
 *
 * @author stw
 *
 */
public class DuplicateMessageException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Records which key was already present in the dedup store
	 *
	 * @param key
	 *            the dedup key that was already known
	 */
	public DuplicateMessageException(final String key) {
		super("Message already processed: " + key, null, false, false);
	}
}
