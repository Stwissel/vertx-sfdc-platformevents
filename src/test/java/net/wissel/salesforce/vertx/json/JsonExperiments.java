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
package net.wissel.salesforce.vertx.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * @author swissel
 *
 */
public class JsonExperiments {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(final String[] args) throws IOException {
		final JsonExperiments j = new JsonExperiments();
		j.test1();

	}

	private JsonObject getScope() {
		final JsonObject jo = new JsonObject();
		jo.put("YesNo", true);
		jo.put("Taste", "Sweet");
		jo.put("Answer", 42);
		jo.put("Deep", new JsonObject().put("under", "the sea"));
		jo.put("TrafficLight", new JsonArray(Arrays.asList("red", "blue", "green")));
		return jo;
	}

	private Mustache getTemplate() {
		final Vertx vertx = Vertx.vertx();
		final FileSystem fs = vertx.fileSystem();
		final Buffer b = fs.readFileBlocking("sample.mustache");
		final MustacheFactory mf = new DefaultMustacheFactory();
		final ByteArrayInputStream bi = new ByteArrayInputStream(b.getBytes());
		final Mustache mustache = mf.compile(new InputStreamReader(bi), "Test");
		return mustache;
	}

	private Writer getWriter() {
		final PrintWriter pw = new PrintWriter(System.out);
		return pw;
	}

	@SuppressWarnings("unchecked")
	private List<Object> mappifyJsonArray(final JsonArray workArray) {
		final List<Object> result = new ArrayList<>();
		workArray.getList().forEach(entry -> {
			if (entry != null) {
				if (entry instanceof JsonArray) {
					result.add(this.mappifyJsonArray((JsonArray) entry));
				} else if (entry instanceof JsonObject) {
					result.add(this.mappifyJsonObject((JsonObject) entry));
				} else {
					result.add(entry);
				}
			}
		});
		return result;
	}

	private Map<String, Object> mappifyJsonObject(final JsonObject sourceJson) {
		final Map<String, Object> result = new HashMap<>();
		final Map<String, Object> sourceMap = sourceJson.getMap();
		sourceMap.forEach((key, value) -> {
			// Check for nested JSONObjects and JSONArrays
			if ((key != null) && (value != null)) {
				if (value instanceof JsonArray) {
					final JsonArray workArray = (JsonArray) value;
					result.put(key, this.mappifyJsonArray(workArray));
				} else if (value instanceof JsonObject) {
					final JsonObject workJson = (JsonObject) value;
					result.put(key, this.mappifyJsonObject(workJson));
				} else {
					result.put(key, value);
				}
			}

		});
		return result;
	}

	private void test1() throws IOException {
		// final JsonObject scope = this.getScope();
		final JsonObject x = this.getScope();
		// Map<String, Object> scope = x.getMap();

		final Map<String, Object> scope = this.mappifyJsonObject(x);
		// Map<String, Object> scope = new HashMap<>();
		// scope.put("YesNo", true);
		// scope.put("answer", 42);
		final Mustache m = this.getTemplate();
		final Writer w = this.getWriter();
		w.append(x.encodePrettily());
		m.execute(w, scope);
		w.close();

	}
}
