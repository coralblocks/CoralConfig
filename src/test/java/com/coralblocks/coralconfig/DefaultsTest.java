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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.coralblocks.coralconfig;

import static com.coralblocks.coralconfig.ConfigKey.*;
import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

public class DefaultsTest {

	private static final class DefaultsHolder {

		public static final ConfigKey<Integer> MY_INTEGER_1 = intKey(10);
		public static final ConfigKey<Integer> MY_INTEGER_2 = intKey(20).deprecated(MY_INTEGER_1);
		public static final ConfigKey<Integer> MY_INTEGER_3 = intKey(30).alias(MY_INTEGER_1);
	}

	private static final class Overwrite {

		private final ConfigKey<Integer> configKey;
		private final int value;

		private Overwrite(ConfigKey<Integer> configKey, int value) {
			this.configKey = configKey;
			this.value = value;
		}
	}

	@Test
	public void testEquivalentNumericGroupDefaultsResolve() {

		class Holder {

			public static final ConfigKey<Integer> PRIMARY = intKey();
			public static final ConfigKey<Integer> OLD_INT = intKey(5).deprecated(PRIMARY);
			public static final ConfigKey<Long> OLD_LONG = longKey(5L).deprecated(PRIMARY);
		}

		MapConfiguration config = new MapConfiguration(Holder.class);

		Assert.assertEquals(5, config.get(Holder.PRIMARY).intValue());
		Assert.assertFalse(config.overwriteDefault(Holder.PRIMARY, 7));
		Assert.assertEquals(7, config.get(Holder.PRIMARY).intValue());
	}

	@Test
	public void testPrimaryOwnDefaultTakesPrecedenceOverGroupDefaults() {

		class Holder {

			public static final ConfigKey<Integer> PRIMARY = intKey(5);
			public static final ConfigKey<Float> OLD = floatKey(7.5f).deprecated(PRIMARY);
		}

		MapConfiguration config = new MapConfiguration(Holder.class);

		Assert.assertEquals(5, config.get(Holder.PRIMARY).intValue());
	}

	@Test
	public void testOverwrittenDefaultAccessorsResolveKeyGroup() {

		class NumericHolder {

			public static final ConfigKey<Integer> PRIMARY = intKey(1);
			public static final ConfigKey<Float> OLD = floatKey().deprecated(PRIMARY);
		}

		MapConfiguration numericConfig = new MapConfiguration(NumericHolder.class);
		numericConfig.overwriteDefault(NumericHolder.OLD, 9f);

		Assert.assertTrue(numericConfig.hasOverwrittenDefault(NumericHolder.PRIMARY));
		Assert.assertEquals(9, numericConfig.getOverwrittenDefault(NumericHolder.PRIMARY).intValue());
		Assert.assertEquals(9, numericConfig.get(NumericHolder.PRIMARY).intValue());

		class NullableHolder {

			public static final ConfigKey<String> PRIMARY = stringKey("default");
			public static final ConfigKey<String> OLD = stringKey().deprecated(PRIMARY);
		}

		MapConfiguration nullableConfig = new MapConfiguration(NullableHolder.class);
		nullableConfig.overwriteDefault(NullableHolder.OLD, null);

		Assert.assertTrue(nullableConfig.hasOverwrittenDefault(NullableHolder.PRIMARY));
		Assert.assertNull(nullableConfig.getOverwrittenDefault(NullableHolder.PRIMARY));
		Assert.assertNull(nullableConfig.get(NullableHolder.PRIMARY));

		MapConfiguration nullableCopy = new MapConfiguration(nullableConfig);

		Assert.assertTrue(nullableCopy.hasOverwrittenDefault(NullableHolder.PRIMARY));
		Assert.assertNull(nullableCopy.getOverwrittenDefault(NullableHolder.PRIMARY));
		Assert.assertNull(nullableCopy.get(NullableHolder.PRIMARY));
	}

	@Test
	public void testNullDefaults() {

		class Base1 {
			public static final ConfigKey<String> MY_STRING = stringKey().def(null);
		}

		MapConfiguration mc1 = new MapConfiguration(Base1.class);
		Assert.assertFalse(Base1.MY_STRING.isRequired());
		Assert.assertNull(mc1.get(Base1.MY_STRING));

		class Base2 {
			public static final ConfigKey<String> MY_STRING = stringKey();
		}

		MapConfiguration mc2 = new MapConfiguration(Base2.class);
		Assert.assertTrue(Base2.MY_STRING.isRequired());

		try {

			mc2.get(Base2.MY_STRING);
			fail();

		} catch(IllegalStateException e) {

			// Expected: the key is required.
		}
	}

	@Test
	public void testBasicsOfBasics() {

		class Base {
			public static final ConfigKey<Integer> HEARTBEAT_INTERVAL = intKey(5);
			public static final ConfigKey<Integer> HEARTBEAT = intKey().alias(HEARTBEAT_INTERVAL);
		}

		MapConfiguration mc = new MapConfiguration(Base.class);
		Assert.assertEquals(5, mc.get(Base.HEARTBEAT_INTERVAL).intValue());
		Assert.assertEquals(5, mc.get(Base.HEARTBEAT).intValue());

		mc.add(Base.HEARTBEAT_INTERVAL, 2);
		Assert.assertEquals(2, mc.get(Base.HEARTBEAT_INTERVAL).intValue());
		Assert.assertEquals(2, mc.get(Base.HEARTBEAT).intValue());

		mc.remove(Base.HEARTBEAT_INTERVAL);
		mc.add(Base.HEARTBEAT, 1);
		Assert.assertEquals(1, mc.get(Base.HEARTBEAT_INTERVAL).intValue());
		Assert.assertEquals(1, mc.get(Base.HEARTBEAT).intValue());
	}

	@Test
	public void testEnum() {

		enum TestEnum { BALL, BOB, BILL }

		class Base {
			public static final ConfigKey<TestEnum> MY_ENUM = enumKey(TestEnum.class, TestEnum.BOB);
		}

		MapConfiguration mc = new MapConfiguration(Base.class);

		Assert.assertEquals(TestEnum.BOB, mc.get(Base.MY_ENUM));

		mc.overwriteDefault(Base.MY_ENUM, TestEnum.BILL);
		Assert.assertEquals(TestEnum.BILL, mc.get(Base.MY_ENUM));

		mc.add(Base.MY_ENUM, TestEnum.BALL);
		Assert.assertEquals(TestEnum.BALL, mc.get(Base.MY_ENUM));

		mc.remove(Base.MY_ENUM);
		Assert.assertEquals(TestEnum.BILL, mc.get(Base.MY_ENUM));

		mc.overwriteDefault(Base.MY_ENUM, null);
		Assert.assertNull(mc.get(Base.MY_ENUM));

		mc.removeOverwrittenDefault(Base.MY_ENUM);
		Assert.assertEquals(TestEnum.BOB, mc.get(Base.MY_ENUM));
	}

	@Test
	public void testEnumNullDefault() {

		enum TestEnum { BALL, BOB, BILL }

		class Base {
			public static final ConfigKey<TestEnum> MY_ENUM = enumKey(TestEnum.class).def(null);
		}

		MapConfiguration mc = new MapConfiguration(Base.class);

		Assert.assertNull(mc.get(Base.MY_ENUM));

		mc.overwriteDefault(Base.MY_ENUM, TestEnum.BILL);
		Assert.assertEquals(TestEnum.BILL, mc.get(Base.MY_ENUM));

		mc.add(Base.MY_ENUM, TestEnum.BALL);
		Assert.assertEquals(TestEnum.BALL, mc.get(Base.MY_ENUM));

		mc.remove(Base.MY_ENUM);
		Assert.assertEquals(TestEnum.BILL, mc.get(Base.MY_ENUM));

		mc.overwriteDefault(Base.MY_ENUM, null);
		Assert.assertNull(mc.get(Base.MY_ENUM));

		mc.removeOverwrittenDefault(Base.MY_ENUM);
		Assert.assertNull(mc.get(Base.MY_ENUM));
	}

	@Test
	public void testOverwrittenDefaultResolutionTable() {

		assertOverwrittenDefaults("declared defaults", 10, 20, 30);
		assertOverwrittenDefaults("primary", 11, 11, 11,
				overwrite(DefaultsHolder.MY_INTEGER_1, 11));
		assertOverwrittenDefaults("deprecated", 22, 22, 22,
				overwrite(DefaultsHolder.MY_INTEGER_2, 22));
		assertOverwrittenDefaults("alias", 33, 33, 33,
				overwrite(DefaultsHolder.MY_INTEGER_3, 33));
		assertOverwrittenDefaults("all keys", 66, 77, 88,
				overwrite(DefaultsHolder.MY_INTEGER_1, 66),
				overwrite(DefaultsHolder.MY_INTEGER_2, 77),
				overwrite(DefaultsHolder.MY_INTEGER_3, 88));
		assertOverwrittenDefaults("primary and deprecated", 66, 77, 66,
				overwrite(DefaultsHolder.MY_INTEGER_1, 66),
				overwrite(DefaultsHolder.MY_INTEGER_2, 77));
		assertOverwrittenDefaults("primary and alias", 66, 66, 77,
				overwrite(DefaultsHolder.MY_INTEGER_1, 66),
				overwrite(DefaultsHolder.MY_INTEGER_3, 77));
		assertOverwrittenDefaults("deprecated then alias", 77, 66, 77,
				overwrite(DefaultsHolder.MY_INTEGER_2, 66),
				overwrite(DefaultsHolder.MY_INTEGER_3, 77));
		assertOverwrittenDefaults("alias then deprecated", 77, 66, 77,
				overwrite(DefaultsHolder.MY_INTEGER_3, 77),
				overwrite(DefaultsHolder.MY_INTEGER_2, 66));
		assertOverwrittenDefaults("alias then primary", 66, 66, 77,
				overwrite(DefaultsHolder.MY_INTEGER_3, 77),
				overwrite(DefaultsHolder.MY_INTEGER_1, 66));
		assertOverwrittenDefaults("alias only", 77, 77, 77,
				overwrite(DefaultsHolder.MY_INTEGER_3, 77));
		assertOverwrittenDefaults("deprecated only", 77, 77, 77,
				overwrite(DefaultsHolder.MY_INTEGER_2, 77));
	}

	@Test
	public void testConfiguredValueResolutionTable() {

		assertConfiguredValues("primary configured", "myInteger1=5", 5, 5, 5);
		assertConfiguredValues("deprecated configured", "myInteger2=5", 5, 5, 5);
		assertConfiguredValues("alias configured", "myInteger3=5", 5, 5, 5);
		assertConfiguredValues("deprecated and alias configured", "myInteger2=6 myInteger3=5", 5, 6, 5);
		assertConfiguredValues("primary and alias configured", "myInteger1=5 myInteger3=6", 5, 5, 6);
		assertConfiguredValues("primary and deprecated configured", "myInteger1=5 myInteger2=6", 5, 6, 5);
	}

	private static void assertOverwrittenDefaults(String description,
			int expectedPrimary, int expectedDeprecated, int expectedAlias, Overwrite ... overwrites) {

		MapConfiguration config = new MapConfiguration(DefaultsHolder.class);

		for(Overwrite overwrite : overwrites) {
			config.overwriteDefault(overwrite.configKey, overwrite.value);
		}

		assertValues(description, config, expectedPrimary, expectedDeprecated, expectedAlias);
	}

	private static void assertConfiguredValues(String description, String params,
			int expectedPrimary, int expectedDeprecated, int expectedAlias) {

		MapConfiguration config = new MapConfiguration(params, DefaultsHolder.class);

		assertValues(description + " before overwrites", config,
				expectedPrimary, expectedDeprecated, expectedAlias);

		config.overwriteDefault(DefaultsHolder.MY_INTEGER_1, 66);
		config.overwriteDefault(DefaultsHolder.MY_INTEGER_2, 77);
		config.overwriteDefault(DefaultsHolder.MY_INTEGER_3, 88);

		assertValues(description + " after overwrites", config,
				expectedPrimary, expectedDeprecated, expectedAlias);
	}

	private static Overwrite overwrite(ConfigKey<Integer> configKey, int value) {
		return new Overwrite(configKey, value);
	}

	private static void assertValues(String description, MapConfiguration config,
			int expectedPrimary, int expectedDeprecated, int expectedAlias) {

		Assert.assertEquals(description + " primary", expectedPrimary,
				config.get(DefaultsHolder.MY_INTEGER_1).intValue());
		Assert.assertEquals(description + " deprecated", expectedDeprecated,
				config.get(DefaultsHolder.MY_INTEGER_2).intValue());
		Assert.assertEquals(description + " alias", expectedAlias,
				config.get(DefaultsHolder.MY_INTEGER_3).intValue());
	}
}
