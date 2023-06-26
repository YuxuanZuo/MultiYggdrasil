/*
 * Copyright (C) 2022  Haowei Wen <yushijinhun@gmail.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package xyz.zuoyx.multiyggdrasil.httpd;

import static xyz.zuoyx.multiyggdrasil.util.IOUtils.CONTENT_TYPE_JSON;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.sendResponse;
import static xyz.zuoyx.multiyggdrasil.util.JsonUtils.toJsonString;
import java.io.IOException;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.JsonObject;
import xyz.zuoyx.multiyggdrasil.MultiYggdrasil;
import xyz.zuoyx.multiyggdrasil.transform.PerformanceMetrics;

/**
 * MultiYggdrasil's debug API
 */
public class DebugApiEndpoint {

	public void serve(HttpExchange exchange) throws IOException {
		if (exchange.getRequestURI().getPath().equals("/debug/metrics") && exchange.getRequestMethod().equals("GET")) {
			PerformanceMetrics metrics = MultiYggdrasil.getClassTransformer().performanceMetrics;
			JsonObject response = new JsonObject();
			response.addProperty("totalTime", metrics.getTotalTime());
			response.addProperty("matchTime", metrics.getMatchTime());
			response.addProperty("scanTime", metrics.getScanTime());
			response.addProperty("analysisTime", metrics.getAnalysisTime());
			response.addProperty("classesScanned", metrics.getClassesScanned());
			response.addProperty("classesSkipped", metrics.getClassesSkipped());
			sendResponse(exchange, 200, CONTENT_TYPE_JSON, toJsonString(response).getBytes());
		} else {
			sendResponse(exchange, 404, null, null);
		}
	}
}
