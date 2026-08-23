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

import org.junit.Assert;
import org.junit.Test;


public class ConfigContainerTest {
	
	@Test
	public void testBasics() {
		
		class Base1 {
			
			public static final ConfigKey<Integer> TIMEOUT = ConfigKey.intKey();
			
			public static final ConfigKey<Boolean> NO_REWIND = ConfigKey.boolKey();
		}
		
		class Blah {
		
			public static final ConfigKey<Integer> TIMEOUT = ConfigKey.intKey();
		}
		
		ConfigContainer cc1 = ConfigContainer.of(Base1.class);
		Assert.assertSame(cc1, ConfigContainer.of(Base1.class));
		
		Assert.assertEquals(2, cc1.size());
		Assert.assertEquals(Base1.TIMEOUT, cc1.get("timeout"));
		Assert.assertEquals(Base1.NO_REWIND, cc1.get("noRewind"));
		Assert.assertEquals(true, cc1.has(Base1.TIMEOUT));
		Assert.assertEquals(false, cc1.has(Blah.TIMEOUT));
		Assert.assertEquals("TIMEOUT", Base1.TIMEOUT.getFieldName());
		Assert.assertEquals("NO_REWIND", Base1.NO_REWIND.getFieldName());
		Assert.assertEquals(null, Blah.TIMEOUT.getFieldName()); // was not added to any Config
		
		@SuppressWarnings("unused")
		class Base2 {
			
			public static final ConfigKey<Integer> TIMEOUT = ConfigKey.intKey();
			
		}
		
		@SuppressWarnings("unused")
		class Base3 {
			
			public static final ConfigKey<Boolean> TIMEOUT = ConfigKey.boolKey();
		}
		
		try {
		
			ConfigContainer configContainer1 = ConfigContainer.of(Base2.class);
			ConfigContainer configContainer2 = ConfigContainer.of(Base3.class);
			
			ConfigContainer.enforceNoDuplicates(configContainer1, configContainer2);
			
			fail();
			
		} catch(IllegalStateException e) {
			// Good!
		}
	}

	@Test
	public void testNullConfigKeyField() {

		@SuppressWarnings("unused")
		class Holder {

			public static final ConfigKey<Integer> VALID = ConfigKey.intKey();

			public static final ConfigKey<Integer> NULL_KEY = null;
		}

		try {

			ConfigContainer.of(Holder.class);

			fail();

		} catch(IllegalStateException e) {

			Assert.assertEquals("Config key field is null: " + Holder.class.getName() + ".NULL_KEY", e.getMessage());
		}

		Assert.assertEquals(null, Holder.VALID.getFieldName());
		Assert.assertEquals(null, Holder.VALID.getHolder());
	}

	@Test
	public void testNonFinalConfigKeyField() {

		class Holder {

			public static ConfigKey<Integer> MUTABLE = ConfigKey.intKey();
		}

		try {

			ConfigContainer.of(Holder.class);

			fail();

		} catch(IllegalStateException e) {

			Assert.assertEquals("Config key field must be final: " + Holder.class.getName() + ".MUTABLE", e.getMessage());
		}

		Assert.assertEquals(null, Holder.MUTABLE.getFieldName());
		Assert.assertEquals(null, Holder.MUTABLE.getHolder());
	}

	@Test
	public void testRegisterRelationshipsWithoutGhostKeys() {

		class Holder {

			public static final ConfigKey<Integer> PRIMARY = ConfigKey.intKey();
			public static final ConfigKey<Integer> ALIAS_DEFAULT_AFTER = ConfigKey.intKey().alias(PRIMARY).def(1);
			public static final ConfigKey<Integer> ALIAS_DEFAULT_BEFORE = ConfigKey.intKey().def(2).alias(PRIMARY);
			public static final ConfigKey<Integer> DEPRECATED_DEFAULT_AFTER = ConfigKey.intKey().deprecated(PRIMARY).def(3);
			public static final ConfigKey<Integer> DEPRECATED_DEFAULT_BEFORE = ConfigKey.intKey().def(4).deprecated(PRIMARY);
		}

		ConfigContainer.of(Holder.class);

		Assert.assertEquals(2, Holder.PRIMARY.getAliases().size());
		Assert.assertTrue(Holder.PRIMARY.getAliases().contains(Holder.ALIAS_DEFAULT_AFTER));
		Assert.assertTrue(Holder.PRIMARY.getAliases().contains(Holder.ALIAS_DEFAULT_BEFORE));

		Assert.assertEquals(2, Holder.PRIMARY.getDeprecated().size());
		Assert.assertTrue(Holder.PRIMARY.getDeprecated().contains(Holder.DEPRECATED_DEFAULT_AFTER));
		Assert.assertTrue(Holder.PRIMARY.getDeprecated().contains(Holder.DEPRECATED_DEFAULT_BEFORE));
	}

	@Test
	public void testParamNamesAreCaseInsensitive() {

		@SuppressWarnings("unused")
		class DuplicateHolder {

			public static final ConfigKey<Integer> MYKEY = ConfigKey.intKey();
			public static final ConfigKey<Integer> MY_KEY = ConfigKey.intKey();
		}

		try {

			ConfigContainer.of(DuplicateHolder.class);

			fail();

		} catch(IllegalStateException e) {

			Assert.assertTrue(e.getMessage().startsWith("Duplicate config key name:"));
		}

		class Holder1 {

			public static final ConfigKey<Integer> MYKEY = ConfigKey.intKey();
		}

		@SuppressWarnings("unused")
		class Holder2 {

			public static final ConfigKey<Integer> MY_KEY = ConfigKey.intKey();
		}

		try {

			new MapConfiguration(Holder1.class, Holder2.class);

			fail();

		} catch(IllegalStateException e) {

			Assert.assertTrue(e.getMessage().startsWith("Found two keys with the same name!"));
		}

		MapConfiguration config = new MapConfiguration("MyKeY=7", Holder1.class);

		Assert.assertEquals(7, config.get(Holder1.MYKEY).intValue());

		try {

			new MapConfiguration("mykey=7 MYKEY=8", Holder1.class);

			fail();

		} catch(IllegalArgumentException e) {

			Assert.assertEquals("Duplicate config key in params: mykey (first: mykey=7, duplicate: MYKEY=8)", e.getMessage());
		}
	}
}
