/** ========================================================================= *
 * Copyright (C)  2017 Salesforce Inc ( http://www.salesforce.com/ )          *
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

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

public class AuthInfoCodec implements MessageCodec<AuthInfo, AuthInfo>{

	@Override
	public void encodeToWire(Buffer buffer, AuthInfo s) {
		// Easiest ways is using JSON object
	    JsonObject jsonToEncode = new JsonObject();
	    jsonToEncode.put("serverName", s.serverName);
	    jsonToEncode.put("sessionToken", s.sessionToken);

	    // Encode object to string
	    String jsonToStr = jsonToEncode.encode();

	    // Length of JSON: is NOT characters count
	    int length = jsonToStr.getBytes().length;

	    // Write data into given buffer
	    buffer.appendInt(length);
	    buffer.appendString(jsonToStr);
		
	}

	@Override
	public AuthInfo decodeFromWire(int pos, Buffer buffer) {
		// My custom message starting from this *position* of buffer

	    // Length of JSON
	    int length = buffer.getInt(pos);

	    // Get JSON string by it`s length
	    // Jump 4 because getInt() == 4 bytes
	    String jsonStr = buffer.getString(pos+=4, pos+=length);
	    JsonObject contentJson = new JsonObject(jsonStr);

	    // Get fields
	    String serverName = contentJson.getString("serverName");
	    String sessionToken = contentJson.getString("sessionToken");

	    // We can finally create custom message object
	    return new AuthInfo(serverName, sessionToken);
	}

	@Override
	public AuthInfo transform(AuthInfo s) {
		return s; // Nothing to
	}

	@Override
	public String name() {
		return this.getClass().getSimpleName();
	}

	@Override
	public byte systemCodecID() {
		return -1;
	}

}
