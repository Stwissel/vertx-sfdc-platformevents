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
package net.wissel.salesforce.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class UtilsTest {

	@Test
	public void dateStringUsesDefaultFormat() {
		final Calendar cal = Calendar.getInstance();
		cal.set(2026, Calendar.OCTOBER, 13, 14, 5, 9);
		final String result = Utils.getDateString(cal.getTime());
		assertEquals("2026-10-13T14:05:09", result);
	}

	@Test
	public void mappifyConvertsNestedStructures() {
		final JsonObject source = new JsonObject()
				.put("name", "gadget")
				.put("count", 42)
				.put("nested", new JsonObject().put("deep", true))
				.put("list", new JsonArray().add("one").add(new JsonObject().put("two", 2)));

		final Map<String, Object> result = Utils.mappifyJsonObject(source);

		assertEquals("gadget", result.get("name"));
		assertEquals(42, result.get("count"));
		assertInstanceOf(Map.class, result.get("nested"));
		assertEquals(true, ((Map<?, ?>) result.get("nested")).get("deep"));
		assertInstanceOf(List.class, result.get("list"));
		final List<?> list = (List<?>) result.get("list");
		assertEquals("one", list.get(0));
		assertInstanceOf(Map.class, list.get(1));
	}

	@Test
	public void mappifySkipsNullValues() {
		final JsonObject source = new JsonObject().put("real", "value").putNull("ghost");
		final Map<String, Object> result = Utils.mappifyJsonObject(source);
		assertEquals("value", result.get("real"));
		assertFalse(result.containsKey("ghost"));
	}

	@Test
	public void mappifyJsonArrayHandlesNesting() {
		final JsonArray source = new JsonArray()
				.add("plain")
				.add(new JsonArray().add(1).add(2))
				.add(new JsonObject().put("key", "value"));

		final List<Object> result = Utils.mappifyJsonArray(source);

		assertEquals(3, result.size());
		assertEquals("plain", result.get(0));
		assertInstanceOf(List.class, result.get(1));
		assertInstanceOf(Map.class, result.get(2));
	}

	@Test
	public void mustacheObjectDispatchesByType() {
		assertInstanceOf(Map.class, Utils.getMustacheObject(new JsonObject().put("a", 1)));
		assertInstanceOf(List.class, Utils.getMustacheObject(new JsonArray().add(1)));
		// JSON-looking strings get parsed
		assertInstanceOf(Map.class, Utils.getMustacheObject("{\"a\": 1}"));
		assertInstanceOf(List.class, Utils.getMustacheObject("[1, 2]"));
		// Plain strings pass through
		final String plain = "not json at all";
		assertSame(plain, Utils.getMustacheObject(plain));
		// Null passes through
		assertNull(Utils.getMustacheObject(null));
		// Other objects pass through
		final Date d = new Date();
		assertSame(d, Utils.getMustacheObject(d));
	}

	@Test
	public void mergePreservesMissingKeys() {
		final JsonObject base = new JsonObject().put("keep", "old").put("update", "old");
		final JsonObject incoming = new JsonObject().put("update", "new").put("fresh", "value");

		Utils.mergeObjects(base, incoming, JsonMergeOptions.PRESERVE_MISSING);

		assertEquals("old", base.getString("keep"));
		assertEquals("new", base.getString("update"));
		assertEquals("value", base.getString("fresh"));
	}

	@Test
	public void mergeRemovesMissingKeys() {
		final JsonObject base = new JsonObject().put("goner", "old").put("update", "old");
		final JsonObject incoming = new JsonObject().put("update", "new");

		Utils.mergeObjects(base, incoming, JsonMergeOptions.REMOVE_MISSING);

		assertFalse(base.containsKey("goner"));
		assertEquals("new", base.getString("update"));
	}

	@Test
	public void mergeToleratesNullArguments() {
		final JsonObject base = new JsonObject().put("stay", true);
		Utils.mergeObjects(base, null, JsonMergeOptions.REMOVE_MISSING);
		assertTrue(base.getBoolean("stay"));
		// Must not throw
		Utils.mergeObjects(null, new JsonObject(), JsonMergeOptions.PRESERVE_MISSING);
	}
}
