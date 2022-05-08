/*
 * Copyright (C) 2019  Haowei Wen <yushijinhun@gmail.com> and contributors
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
package xyz.zuoyx.multiyggdrasil.internal.fi.iki.elonen;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.asBytes;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

@SuppressWarnings("resource")
public class ChunkedInputStreamTest {

	@Test
	public void testRead1() throws IOException {
		byte[] data = ("4\r\nWiki\r\n5\r\npedia\r\ne\r\n in\r\n\r\nchunks.\r\n0\r\n\r\n").getBytes(US_ASCII);
		ByteArrayInputStream underlying = new ByteArrayInputStream(data);
		InputStream in = new ChunkedInputStream(underlying);
		assertArrayEquals(("Wikipedia in\r\n\r\nchunks.").getBytes(US_ASCII), asBytes(in));
		assertEquals(underlying.read(), -1);
	}

	@Test
	public void testRead2() throws IOException {
		byte[] data = ("4\r\nWiki\r\n5\r\npedia\r\ne\r\n in\r\n\r\nchunks.\r\n0\r\n\r\n.").getBytes(US_ASCII);
		ByteArrayInputStream underlying = new ByteArrayInputStream(data);
		InputStream in = new ChunkedInputStream(underlying);
		assertArrayEquals(("Wikipedia in\r\n\r\nchunks.").getBytes(US_ASCII), asBytes(in));
		assertEquals(underlying.read(), '.');
	}

	@Test
	public void testRead3() throws IOException {
		byte[] data = ("25\r\nThis is the data in the first chunk\r\n\r\n1c\r\nand this is the second one\r\n\r\n3\r\ncon\r\n8\r\nsequence\r\n0\r\n\r\n").getBytes(US_ASCII);
		ByteArrayInputStream underlying = new ByteArrayInputStream(data);
		InputStream in = new ChunkedInputStream(underlying);
		assertArrayEquals(("This is the data in the first chunk\r\nand this is the second one\r\nconsequence").getBytes(US_ASCII), asBytes(in));
		assertEquals(underlying.read(), -1);
	}

	@Test
	public void testRead4() throws IOException {
		byte[] data = ("25\r\nThis is the data in the first chunk\r\n\r\n1C\r\nand this is the second one\r\n\r\n3\r\ncon\r\n8\r\nsequence\r\n0\r\n\r\n.").getBytes(US_ASCII);
		ByteArrayInputStream underlying = new ByteArrayInputStream(data);
		InputStream in = new ChunkedInputStream(underlying);
		assertArrayEquals(("This is the data in the first chunk\r\nand this is the second one\r\nconsequence").getBytes(US_ASCII), asBytes(in));
		assertEquals(underlying.read(), '.');
	}

	@Test
	public void testRead5() throws IOException {
		byte[] data = ("0\r\n\r\n").getBytes(US_ASCII);
		ByteArrayInputStream underlying = new ByteArrayInputStream(data);
		InputStream in = new ChunkedInputStream(underlying);
		assertArrayEquals(new byte[0], asBytes(in));
		assertEquals(underlying.read(), -1);
	}

	@Test
	public void testReadEOF1() {
		assertThrows(EOFException.class, () -> {
			byte[] data = ("a").getBytes(US_ASCII);
			ByteArrayInputStream underlying = new ByteArrayInputStream(data);
			InputStream in = new ChunkedInputStream(underlying);
			asBytes(in);
		});
	}

	@Test
	public void testReadEOF2() {
		assertThrows(EOFException.class, () -> {
			byte[] data = ("a\r").getBytes(US_ASCII);
			ByteArrayInputStream underlying = new ByteArrayInputStream(data);
			InputStream in = new ChunkedInputStream(underlying);
			asBytes(in);
		});
	}

	@Test
	public void testReadEOF3() {
		assertThrows(EOFException.class, () -> {
			byte[] data = ("a\r\n").getBytes(US_ASCII);
			ByteArrayInputStream underlying = new ByteArrayInputStream(data);
			InputStream in = new ChunkedInputStream(underlying);
			asBytes(in);
		});
	}

	@Test
	public void testReadEOF4() {
		assertThrows(EOFException.class, () -> {
			byte[] data = ("a\r\nabc").getBytes(US_ASCII);
			ByteArrayInputStream underlying = new ByteArrayInputStream(data);
			InputStream in = new ChunkedInputStream(underlying);
			asBytes(in);
		});
	}

	@Test
	public void testReadEOF5() {
		assertThrows(EOFException.class, () -> {
			byte[] data = ("a\r\n123456789a\r").getBytes(US_ASCII);
			ByteArrayInputStream underlying = new ByteArrayInputStream(data);
			InputStream in = new ChunkedInputStream(underlying);
			asBytes(in);
		});
	}

	@Test
	public void testReadEOF6() {
		assertThrows(EOFException.class, () -> {
			byte[] data = ("a\r\n123456789a\r\n").getBytes(US_ASCII);
			ByteArrayInputStream underlying = new ByteArrayInputStream(data);
			InputStream in = new ChunkedInputStream(underlying);
			asBytes(in);
		});
	}

	@Test
	public void testReadEOF7() {
		assertThrows(EOFException.class, () -> {
			byte[] data = ("a\r\n123456789a\r\n0\r\n\r").getBytes(US_ASCII);
			ByteArrayInputStream underlying = new ByteArrayInputStream(data);
			InputStream in = new ChunkedInputStream(underlying);
			asBytes(in);
		});
	}

	@Test
	public void testBadIn1() {
		assertThrows(IOException.class, () -> {
			byte[] data = ("-1").getBytes(US_ASCII);
			ByteArrayInputStream underlying = new ByteArrayInputStream(data);
			InputStream in = new ChunkedInputStream(underlying);
			asBytes(in);
		});
	}

	@Test
	public void testBadIn2() {
		assertThrows(IOException.class, () -> {
			byte[] data = ("a\ra").getBytes(US_ASCII);
			ByteArrayInputStream underlying = new ByteArrayInputStream(data);
			InputStream in = new ChunkedInputStream(underlying);
			asBytes(in);
		});
	}

	@Test
	public void testBadIn3() {
		assertThrows(IOException.class, () -> {
			byte[] data = ("a\r\n123456789aa").getBytes(US_ASCII);
			ByteArrayInputStream underlying = new ByteArrayInputStream(data);
			InputStream in = new ChunkedInputStream(underlying);
			asBytes(in);
		});
	}

	@Test
	public void testBadIn4() {
		assertThrows(IOException.class, () -> {
			byte[] data = ("a\r\n123456789a\ra").getBytes(US_ASCII);
			ByteArrayInputStream underlying = new ByteArrayInputStream(data);
			InputStream in = new ChunkedInputStream(underlying);
			asBytes(in);
		});
	}

	@Test
	public void testBadIn5() {
		assertThrows(IOException.class, () -> {
			byte[] data = ("a\r\n123456789a\r\n0\r\n\r-").getBytes(US_ASCII);
			ByteArrayInputStream underlying = new ByteArrayInputStream(data);
			InputStream in = new ChunkedInputStream(underlying);
			asBytes(in);
		});
	}
}
