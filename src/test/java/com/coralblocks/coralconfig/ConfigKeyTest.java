/* 
 * Copyright 2015-2025 (c) CoralBlocks LLC - http://www.coralblocks.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.coralblocks.coralconfig;

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

public class ConfigKeyTest {
	
	static enum TestEnum {
		BLAH, FOO, CAT, DOG
	}

	static enum MixedCaseEnum {
		BILL, CamelCase
	}
	
	@Test
	public void testBasics() {
		
		ConfigKey<Integer> intKey = ConfigKey.intKey();
		
		Integer value = intKey.parseValue("3");
		
		Assert.assertEquals(3, value.intValue());
		Assert.assertEquals(null, intKey.getFieldName());
		
		ConfigKey<TestEnum> enumKey = ConfigKey.enumKey(TestEnum.class);
		
		TestEnum testEnum = enumKey.parseValue("BLAH");
		Assert.assertEquals(TestEnum.BLAH, testEnum);
		
		testEnum = enumKey.parseValue("BLaH");
		Assert.assertEquals(TestEnum.BLAH, testEnum);
		
		testEnum = enumKey.parseValue("cat");
		Assert.assertEquals(TestEnum.CAT, testEnum);
		
		testEnum = enumKey.parseValue("Cat");
		Assert.assertEquals(TestEnum.CAT, testEnum);
		
		try {
			testEnum = enumKey.parseValue("Catty");
			fail();
		} catch(IllegalArgumentException e) {
			// Nice
		}
	}

	@Test
	public void testEnumParsingIsCaseInsensitiveAndLocaleIndependent() {

		ConfigKey<MixedCaseEnum> enumKey = ConfigKey.enumKey(MixedCaseEnum.class);

		Assert.assertEquals(MixedCaseEnum.CamelCase, enumKey.parseValue("CamelCase"));
		Assert.assertEquals(MixedCaseEnum.CamelCase, enumKey.parseValue("camelcase"));

		Locale defaultLocale = Locale.getDefault();

		try {

			Locale.setDefault(Locale.forLanguageTag("tr-TR"));

			Assert.assertEquals(MixedCaseEnum.BILL, enumKey.parseValue("bill"));

		} finally {

			Locale.setDefault(defaultLocale);
		}
	}

	@Test
	public void testLeadingUnderscoreIsIgnoredInParamName() {

		class Holder {

			public static final ConfigKey<Integer> _MY_KEY = ConfigKey.intKey();
		}

		ConfigContainer.of(Holder.class);

		Assert.assertEquals("myKey", Holder._MY_KEY.getParamName());
	}

	@Test
	public void testBooleanParsingIsStrict() {

		ConfigKey<Boolean> boolKey = ConfigKey.boolKey();

		Assert.assertEquals(true, boolKey.parseValue("TrUe"));
		Assert.assertEquals(false, boolKey.parseValue("FaLsE"));

		try {

			boolKey.parseValue("ture");

			fail();

		} catch(IllegalArgumentException e) {

			Assert.assertEquals("Invalid boolean value: ture", e.getMessage());
		}
	}

	@Test
	public void testDescriptionSurvivesFluentCalls() {

		class Holder {

			public static final ConfigKey<String> PRIMARY = ConfigKey.stringKey();
			public static final ConfigKey<String> DEFAULT = ConfigKey.stringKey().setDescription("default").def("value");
			public static final ConfigKey<String> ALIAS = ConfigKey.stringKey().setDescription("alias").alias(PRIMARY);
			public static final ConfigKey<String> DEPRECATED = ConfigKey.stringKey().setDescription("deprecated").deprecated(PRIMARY);
		}

		ConfigContainer.of(Holder.class);

		Assert.assertEquals("default", Holder.DEFAULT.getDescription());
		Assert.assertEquals("alias", Holder.ALIAS.getDescription());
		Assert.assertEquals("deprecated", Holder.DEPRECATED.getDescription());

		ConfigKey<String> configKey = ConfigKey.stringKey();
		Assert.assertSame(configKey, configKey.setDescription("mutated"));
	}

	@Test
	public void testRelationshipListsAreNeverNullOrMutable() {

		class Holder {

			public static final ConfigKey<Integer> PRIMARY = ConfigKey.intKey();
			public static final ConfigKey<Integer> ALIAS = ConfigKey.intKey().alias(PRIMARY);
			public static final ConfigKey<Integer> DEPRECATED = ConfigKey.intKey().deprecated(PRIMARY);
		}

		Assert.assertTrue(Holder.ALIAS.getAliases().isEmpty());
		Assert.assertTrue(Holder.ALIAS.getDeprecated().isEmpty());
		Assert.assertThrows(UnsupportedOperationException.class,
				() -> Holder.PRIMARY.getAliases().add(Holder.ALIAS));

		ConfigContainer.of(Holder.class);

		Assert.assertEquals(1, Holder.PRIMARY.getAliases().size());
		Assert.assertEquals(1, Holder.PRIMARY.getDeprecated().size());
		Assert.assertThrows(UnsupportedOperationException.class,
				() -> Holder.PRIMARY.getDeprecated().clear());
	}

	@Test
	public void testIncompatibleDeprecationAfterPrimaryHolderIsScanned() {

		class Holder {

			public static final ConfigKey<Integer> PRIMARY = ConfigKey.intKey();
		}

		ConfigContainer.of(Holder.class);

		try {

			ConfigKey.stringKey().deprecated(Holder.PRIMARY);

			fail();

		} catch(IllegalStateException e) {

			Assert.assertTrue(e.getMessage().startsWith("The types are incompatible for deprecation!"));
		}
	}
}
