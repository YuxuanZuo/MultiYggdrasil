/*
 * Copyright (C) 2020  Haowei Wen <yushijinhun@gmail.com> and contributors
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
package xyz.zuoyx.multiyggdrasil.transform.support;

import java.util.Optional;

import xyz.zuoyx.multiyggdrasil.httpd.URLProcessor;
import xyz.zuoyx.multiyggdrasil.transform.LdcTransformUnit;

public class ConstantURLTransformUnit extends LdcTransformUnit {

	private URLProcessor urlProcessor;

	public ConstantURLTransformUnit(URLProcessor urlProcessor) {
		this.urlProcessor = urlProcessor;
	}

	@Override
	protected Optional<String> transformLdc(String input) {
		return urlProcessor.transformURL(input);
	}

	@Override
	public String toString() {
		return "Constant URL Transformer";
	}
}
