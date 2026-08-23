/* 
 * Copyright 2015-2025 (c) CoralBlocks LLC - http://www.coralblocks.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.coralblocks.coralconfig;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

public class ConfigPrinterTest {

	public static class PrinterHolder {

		public static final ConfigKey<Integer> PRIMARY = ConfigKey.intKey(1);
		public static final ConfigKey<Integer> DEPRECATED = ConfigKey.intKey().deprecated(PRIMARY).def(2);
		public static final ConfigKey<String> TEXT = ConfigKey.stringKey("a,b").setDescription("A \"quoted\", description");
	}

	@Test
	public void testPrintedConfiguration() {

		PrintStream originalOut = System.out;
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {

			System.setOut(capture);
			ConfigPrinter.printConfigs(true, true, true, PrinterHolder.class);

		} finally {

			System.setOut(originalOut);
		}

		String newline = System.lineSeparator();
		String expected = "Field Name,Param Name,Type,Default Value,Holder Class,Kind,Parent Primary,Aliases,Deprecated,Description" + newline
				+ "DEPRECATED,deprecated,Integer,2,PrinterHolder,DEPRECATED,PRIMARY,,," + newline
				+ "PRIMARY,primary,Integer,1,PrinterHolder,PRIMARY,,,DEPRECATED," + newline
				+ "TEXT,text,String,\"a,b\",PrinterHolder,PRIMARY,,,,\"A \"\"quoted\"\", description\"" + newline;

		Assert.assertEquals(expected, output.toString(StandardCharsets.UTF_8));
		Assert.assertEquals("PRIMARY", PrinterHolder.PRIMARY.getFieldName());
		Assert.assertEquals("DEPRECATED", PrinterHolder.DEPRECATED.getFieldName());
		Assert.assertEquals("TEXT", PrinterHolder.TEXT.getFieldName());
	}

	@Test
	public void testBooleanArgumentsAreStrict() {

		assertInvalidBoolean(new String[] { "ture", "false", "false", "unused" },
							 "Invalid boolean value for includeHeaderLine: ture");
		assertInvalidBoolean(new String[] { "includeHeaderLine=true", "includeParamName=fasle", "includeHolderClass=false", "unused" },
							 "Invalid boolean value for includeParamName: fasle");
		assertInvalidBoolean(new String[] { "includeHeaderLine=true", "includeParamName=false", "includeHolderClass=yes", "unused" },
							 "Invalid boolean value for includeHolderClass: yes");
	}

	private static void assertInvalidBoolean(String[] args, String expectedMessage) {

		try {

			ConfigPrinter.main(args);

			fail();

		} catch(IllegalArgumentException e) {

			Assert.assertEquals(expectedMessage, e.getMessage());
		}
	}
}
