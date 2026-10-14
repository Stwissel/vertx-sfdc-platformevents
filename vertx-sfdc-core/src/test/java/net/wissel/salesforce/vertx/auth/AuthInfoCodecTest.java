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
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import io.vertx.core.buffer.Buffer;

public class AuthInfoCodecTest {

	private final AuthInfoCodec codec = new AuthInfoCodec();

	@Test
	public void roundTripFromBufferStart() {
		final AuthInfo source = new AuthInfo("instance.my.salesforce.com", "00D123!AQEAX");
		final Buffer buffer = Buffer.buffer();
		this.codec.encodeToWire(buffer, source);

		final AuthInfo decoded = this.codec.decodeFromWire(0, buffer);

		assertEquals(source.serverName, decoded.serverName);
		assertEquals(source.sessionToken, decoded.sessionToken);
	}

	@Test
	public void roundTripFromNonZeroPosition() {
		// On the wire, the codec payload starts at an arbitrary position
		final Buffer buffer = Buffer.buffer();
		buffer.appendString("leading noise");
		final int pos = buffer.length();

		final AuthInfo source = new AuthInfo("server", "token");
		this.codec.encodeToWire(buffer, source);

		final AuthInfo decoded = this.codec.decodeFromWire(pos, buffer);

		assertEquals("server", decoded.serverName);
		assertEquals("token", decoded.sessionToken);
	}

	@Test
	public void roundTripSurvivesNonAsciiToken() {
		// Byte length != char count for non-ASCII content
		final AuthInfo source = new AuthInfo("sërver.säles.com", "tøken-émoji-日本");
		final Buffer buffer = Buffer.buffer();
		this.codec.encodeToWire(buffer, source);

		final AuthInfo decoded = this.codec.decodeFromWire(0, buffer);

		assertEquals(source.serverName, decoded.serverName);
		assertEquals(source.sessionToken, decoded.sessionToken);
	}

	@Test
	public void transformPassesSameInstance() {
		final AuthInfo source = new AuthInfo("server", "token");
		assertSame(source, this.codec.transform(source));
	}

	@Test
	public void codecIdentity() {
		assertEquals("AuthInfoCodec", this.codec.name());
		assertEquals(-1, this.codec.systemCodecID());
	}
}
