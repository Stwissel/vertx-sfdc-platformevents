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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * @author swissel
 *
 */
public class Utils {

	/**
	 * Standard way to deal with date
	 *
	 * @param date a java date
	 * @return a formatted date string
	 */
	public static String getDateString(final Date date) {
		final SimpleDateFormat sdf = new SimpleDateFormat(Constants.DEFAULT_DATE_FORMAT);
		return sdf.format(date);
	}

	/**
	 * Utility function, to provide something that the Mustache template engine
	 * can digest. Mainly to convert JsonObject and JsonArrays. Also supports
	 * String that look like JSON by creating a JsonObject first. Passes through
	 * other objects
	 *
	 * @param source
	 *            a JSONObject, JsonArray or String
	 * @return A map usable for Mustach template operations
	 */
	public static Object getMustacheObject(final Object source) {

		if (source != null) {
			if (source instanceof JsonObject) {
				return Utils.mappifyJsonObject((JsonObject) source);
			} else if (source instanceof JsonArray) {
				return Utils.mappifyJsonArray((JsonArray) source);
			} else if (source instanceof String) {
				// We try to convert the string to a JsonObject or a JsonArray
				// both failing we return the String
				try {
					final JsonObject jo = new JsonObject(String.valueOf(source));
					return Utils.mappifyJsonObject(jo);
				} catch (final DecodeException de) {
					try {
						final JsonArray ja = new JsonArray(String.valueOf(source));
						return Utils.mappifyJsonArray(ja);
					} catch (final DecodeException de2) {
						// Not JSONy - we leave it as it is
						return source;
					}
				}
			}
		}

		// Simple passing through if it wasn't a special case
		return source;
	}

	@SuppressWarnings("unchecked")
	public static List<Object> mappifyJsonArray(final JsonArray workArray) {
		final List<Object> result = new ArrayList<>();
		workArray.getList().forEach(entry -> {
			if (entry != null) {
				if (entry instanceof JsonArray) {
					result.add(Utils.mappifyJsonArray((JsonArray) entry));
				} else if (entry instanceof JsonObject) {
					result.add(Utils.mappifyJsonObject((JsonObject) entry));
				} else {
					result.add(entry);
				}
			}
		});
		return result;
	}

	public static Map<String, Object> mappifyJsonObject(final JsonObject sourceJson) {
		final Map<String, Object> result = new LinkedHashMap<>();
		final Map<String, Object> sourceMap = sourceJson.getMap();
		sourceMap.forEach((key, value) -> {
			// Check for nested JSONObjects and JSONArrays
			if ((key != null) && (value != null)) {
				if (value instanceof JsonArray) {
					final JsonArray workArray = (JsonArray) value;
					result.put(key, Utils.mappifyJsonArray(workArray));
				} else if (value instanceof JsonObject) {
					final JsonObject workJson = (JsonObject) value;
					result.put(key, Utils.mappifyJsonObject(workJson));
				} else {
					result.put(key, value);
				}
			}

		});
		return result;
	}

}
