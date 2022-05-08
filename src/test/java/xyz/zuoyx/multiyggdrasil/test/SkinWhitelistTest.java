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
package xyz.zuoyx.multiyggdrasil.test;

import static xyz.zuoyx.multiyggdrasil.transform.support.SkinWhitelistTransformUnit.domainMatches;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class SkinWhitelistTest {

	@Test
	public void testEmptyPattern() {
		assertFalse(domainMatches("", "example.com"));
	}

	@Test
	public void testDotMatchesSubdomain() {
		assertTrue(domainMatches(".example.com", "a.example.com"));
	}

	@Test
	public void testDotMatchesSubdomain2() {
		assertTrue(domainMatches(".example.com", "b.a.example.com"));
	}

	@Test
	public void testDotNotMatchesToplevel() {
		assertFalse(domainMatches(".example.com", "example.com"));
	}

	@Test
	public void testNonDotMatchesToplevel() {
		assertTrue(domainMatches("example.com", "example.com"));
	}

	@Test
	public void testNonDotNotMatchesSubdomain() {
		assertFalse(domainMatches("example.com", "a.example.com"));
	}

	@Test
	public void testNonDotNotMatchesOther() {
		assertFalse(domainMatches("example.com", "eexample.com"));
	}
}
