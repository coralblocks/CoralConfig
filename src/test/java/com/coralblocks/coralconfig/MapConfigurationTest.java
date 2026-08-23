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

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class MapConfigurationTest {
	
	static enum TestEnum {
		BALL, BOB, BILLY
	}
	
	public static final ConfigKey<Integer> TIMEOUT = ConfigKey.intKey().def(1);
	public static final ConfigKey<Integer> TIMEOUT_222 = ConfigKey.intKey().deprecated(TIMEOUT).def(22);
	public static final ConfigKey<Integer> TIMEOUT_333 = ConfigKey.intKey().def(22).alias(TIMEOUT);
	
	public static final ConfigKey<Boolean> NO_REWIND = ConfigKey.boolKey();
	
	public static final ConfigKey<TestEnum> MY_ENUM = ConfigKey.enumKey(TestEnum.class).def(TestEnum.BILLY);
	
	@Test
	public void testDefaults() {
		
		MapConfiguration mapConfig = new MapConfiguration("myEnum=BALL noRewind=false", MapConfigurationTest.class);
		
		Assert.assertEquals(2, mapConfig.size());
		Assert.assertEquals(MapConfigurationTest.class, mapConfig.getHolders()[0]);
		Assert.assertEquals(2, mapConfig.keys().size());
		Assert.assertEquals(true, mapConfig.has(MY_ENUM));
		Assert.assertEquals(TestEnum.BALL, mapConfig.get(MY_ENUM));
		Assert.assertEquals(false, mapConfig.has(TIMEOUT));
		Assert.assertEquals(false, mapConfig.get(NO_REWIND));
		
		mapConfig.overwriteDefault(TIMEOUT, 2);
		Assert.assertEquals(2, mapConfig.get(TIMEOUT).intValue());
		
		mapConfig.overwriteDefault(TIMEOUT, 1);
		Assert.assertEquals(1, mapConfig.get(TIMEOUT).intValue());
		
		mapConfig.overwriteDefault(TIMEOUT, 3);
		Assert.assertEquals(3, mapConfig.get(TIMEOUT).intValue());
		
		mapConfig.removeOverwrittenDefault(TIMEOUT);
		Assert.assertEquals(1, mapConfig.get(TIMEOUT).intValue());
		
		try {
			mapConfig.overwriteDefault(TIMEOUT, null); // null default value not allowed for Integer
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
		
		mapConfig.overwriteDefault(TIMEOUT, 222);
		Assert.assertEquals(222, mapConfig.get(TIMEOUT).intValue());
		
		mapConfig.overwriteDefault(MY_ENUM, null);
		Assert.assertEquals(TestEnum.BALL, mapConfig.get(MY_ENUM)); // still has it!
		
		mapConfig.remove(MY_ENUM);
		Assert.assertEquals(null, mapConfig.get(MY_ENUM)); // don't have it anymore! apply overwrite!
	}

	@Test
	public void testParamsParsing() {

		class Holder {

			public static final ConfigKey<Integer> OK_KEY = ConfigKey.intKey();
			public static final ConfigKey<String> MY_STRING = ConfigKey.stringKey();
		}

		MapConfiguration config = new MapConfiguration("  okKey=1 myString=a=b  ", Holder.class);

		Assert.assertEquals(1, config.get(Holder.OK_KEY).intValue());
		Assert.assertEquals("a=b", config.get(Holder.MY_STRING));

		config = new MapConfiguration("myString=", Holder.class);
		Assert.assertEquals("", config.get(Holder.MY_STRING));

		config = new MapConfiguration("   ", Holder.class);
		Assert.assertEquals(0, config.size());

		try {

			new MapConfiguration("okKey=1 okKey=2", Holder.class);

			fail();

		} catch(IllegalArgumentException e) {

			Assert.assertEquals("Duplicate config key in params: okKey (first: okKey=1, duplicate: okKey=2)", e.getMessage());
		}
	}
	
	@Test
	public void testBasics1() {
		
		MapConfiguration config = new MapConfiguration("myEnum=BALL noRewind=false", MapConfigurationTest.class);
		
		Assert.assertEquals(2, config.size());
		Assert.assertEquals(MapConfigurationTest.class, config.getHolders()[0]);
		Assert.assertEquals(2, config.keys().size());
		Assert.assertEquals(true, config.has(MY_ENUM));
		Assert.assertEquals(TestEnum.BALL, config.get(MY_ENUM));
		Assert.assertEquals(false, config.has(TIMEOUT));
		Assert.assertEquals(false, config.get(NO_REWIND));
		
		config.remove(NO_REWIND);
		
		try {
			config.get(NO_REWIND);
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
	}
	
	@Test
	public void testBasics2() {
		
		MapConfiguration mc = new MapConfiguration("myEnum=BALL noRewind=false", MapConfigurationTest.class);
		
		Configuration config = new MapConfiguration(mc);
		
		mc.remove(MY_ENUM); // won't affect the new configuration
		
		Assert.assertEquals(2, config.size());
		Assert.assertEquals(MapConfigurationTest.class, config.getHolders()[0]);
		Assert.assertEquals(2, config.keys().size());
		Assert.assertEquals(true, config.has(MY_ENUM));
		Assert.assertEquals(TestEnum.BALL, config.get(MY_ENUM));
		Assert.assertEquals(false, config.has(TIMEOUT));
		Assert.assertEquals(false, config.get(NO_REWIND));
		
		mc.remove(NO_REWIND);
		
		try {
			mc.get(NO_REWIND);
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
	}

	@Test
	public void testHolderArraysAreDefensivelyCopied() {

		class Holder {

			public static final ConfigKey<Integer> KEY = ConfigKey.intKey();
		}

		Class<?>[] constructorHolders = { Holder.class };
		MapConfiguration config = new MapConfiguration(constructorHolders);

		constructorHolders[0] = String.class;
		Assert.assertEquals(Holder.class, config.getHolders()[0]);

		Class<?>[] returnedHolders = config.getHolders();
		returnedHolders[0] = String.class;
		Assert.assertEquals(Holder.class, config.getHolders()[0]);

		Class<?>[] sourceHolders = { Holder.class };
		Configuration source = Mockito.mock(Configuration.class);
		Mockito.when(source.getHolders()).thenReturn(sourceHolders);
		Mockito.when(source.keys()).thenReturn(Collections.emptySet());
		Mockito.when(source.keysWithOverwrittenDefault()).thenReturn(Collections.emptySet());

		MapConfiguration copy = new MapConfiguration(source);

		sourceHolders[0] = String.class;
		Assert.assertEquals(Holder.class, copy.getHolders()[0]);
	}
	
	@Test
	public void testBasics3() {
		
		MapConfiguration mapConfig = new MapConfiguration("myEnum=BALL noRewind=false", MapConfigurationTest.class);
		
		try {
			mapConfig.add(TIMEOUT, null); // null values are not allowed
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
		
		TestEnum prevEnum = mapConfig.add(MY_ENUM, TestEnum.BILLY);
		Assert.assertEquals(TestEnum.BALL, prevEnum);
		
		boolean prevNoRewind = mapConfig.remove(NO_REWIND);
		Assert.assertEquals(false, prevNoRewind);
		Assert.assertEquals(1, mapConfig.size());
		
		try {
			@SuppressWarnings("unused")
			int timeout = mapConfig.remove(TIMEOUT); // autoboxing NPE
			fail();
		} catch(NullPointerException e) {
			// Good
		}
		
		try {
			mapConfig.remove(TIMEOUT); // NO autoboxing NPE
		} catch(NullPointerException e) {
			fail();
		}
		
		Object prevTimeout = mapConfig.add(TIMEOUT, 33);
		Assert.assertEquals(null, prevTimeout);
		
		Assert.assertEquals(2, mapConfig.size());
		Assert.assertEquals(MapConfigurationTest.class, mapConfig.getHolders()[0]);
		Assert.assertEquals(2, mapConfig.keys().size());
		Assert.assertEquals(true, mapConfig.has(MY_ENUM));
		Assert.assertEquals(TestEnum.BILLY, mapConfig.get(MY_ENUM));
		Assert.assertEquals(false, mapConfig.has(NO_REWIND));
		Assert.assertEquals(true, mapConfig.has(TIMEOUT));
		Assert.assertEquals(33, mapConfig.get(TIMEOUT).intValue());
		
		try {
			mapConfig.get(NO_REWIND);
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
	}
	
	@Test
	public void testGetAliasesAndDeprecated() {
		
		class Base1 {
			
			public static final ConfigKey<Boolean> NO_REWIND = ConfigKey.boolKey();
			public static final ConfigKey<Boolean> NO_REWIND1 = ConfigKey.boolKey().alias(NO_REWIND);
			public static final ConfigKey<Boolean> NO_REWIND2 = ConfigKey.boolKey().alias(NO_REWIND);
			public static final ConfigKey<Boolean> IS_NO_REWIND = ConfigKey.boolKey().deprecated(NO_REWIND);

			public static final ConfigKey<Integer> TIME_INTEGER = ConfigKey.intKey();
			public static final ConfigKey<Float> TIME_FLOAT = ConfigKey.floatKey().deprecated(TIME_INTEGER);
			
			public static final ConfigKey<Double> PRICE_DOUBLE = ConfigKey.doubleKey(1.1111d);
			public static final ConfigKey<Integer> PRICE_INT = ConfigKey.intKey().def(123).deprecated(PRICE_DOUBLE);
		}
		
		MapConfiguration config = new MapConfiguration(Base1.class);
		
		Assert.assertEquals(8, config.allConfigKeys().size());
		
		config.add(Base1.PRICE_INT, 2);
		Assert.assertTrue(2d == config.get(Base1.PRICE_DOUBLE));
		Assert.assertEquals(2, config.get(Base1.PRICE_INT).intValue());
		
		config.add(Base1.PRICE_DOUBLE, 2.3423d);
		Assert.assertTrue(2.3423d == config.get(Base1.PRICE_DOUBLE));
		Assert.assertEquals(2, config.get(Base1.PRICE_INT).intValue());
		
		config.remove(Base1.PRICE_DOUBLE);
		Assert.assertTrue(2d == config.get(Base1.PRICE_DOUBLE));
		Assert.assertEquals(2, config.get(Base1.PRICE_INT).intValue());
		
		config.remove(Base1.PRICE_INT);
		Assert.assertTrue(1.1111d == config.get(Base1.PRICE_DOUBLE));
		Assert.assertEquals(123, config.get(Base1.PRICE_INT).intValue());
		
		config.add(Base1.IS_NO_REWIND, true);
		Assert.assertEquals(true, config.get(Base1.NO_REWIND));
		
		config.add(Base1.NO_REWIND1, false);
		Assert.assertEquals(false, config.get(Base1.NO_REWIND));
		
		config.add(Base1.NO_REWIND2, true);
		Assert.assertEquals(false, config.get(Base1.NO_REWIND));
		
		config.remove(Base1.NO_REWIND1);
		Assert.assertEquals(true, config.get(Base1.NO_REWIND));
		
		config.remove(Base1.NO_REWIND2);
		Assert.assertEquals(true, config.get(Base1.NO_REWIND));
		
		config.add(Base1.IS_NO_REWIND, false);
		Assert.assertEquals(false, config.get(Base1.NO_REWIND));
	
		config.add(Base1.TIME_FLOAT, 2.84234f);
		try {
			config.get(Base1.TIME_INTEGER);
			fail();
		} catch(IllegalArgumentException e) {
			Assert.assertTrue(e.getMessage().startsWith("Cannot convert numeric value without loss!"));
		}
		Assert.assertTrue(2.84234f == config.get(Base1.TIME_FLOAT).floatValue());

		config.add(Base1.TIME_FLOAT, 2f);
		Assert.assertEquals(2, config.get(Base1.TIME_INTEGER).intValue());
		
		config.add(Base1.TIME_INTEGER, 3);
		Assert.assertEquals(3, config.get(Base1.TIME_INTEGER).intValue());
		Assert.assertTrue(2f == config.get(Base1.TIME_FLOAT).floatValue());
		
		@SuppressWarnings("unused")
		class Base2 {
			
			public static final ConfigKey<Boolean> NO_REWIND22 = ConfigKey.boolKey();
			public static final ConfigKey<Boolean> NO_REWIND33 = ConfigKey.boolKey().alias(NO_REWIND22);
		}
		
		MapConfiguration mc = new MapConfiguration(Base1.class, Base2.class);
		Assert.assertEquals(10, mc.allConfigKeys().size());
	}

	@Test
	public void testNumericCoercionRejectsLoss() {

		class Holder {

			public static final ConfigKey<Byte> SMALL = ConfigKey.byteKey();
			public static final ConfigKey<Integer> BIG = ConfigKey.intKey().deprecated(SMALL);
			public static final ConfigKey<Float> FLOAT = ConfigKey.floatKey();
			public static final ConfigKey<Double> DOUBLE = ConfigKey.doubleKey().deprecated(FLOAT);
		}

		MapConfiguration config = new MapConfiguration(Holder.class);

		config.add(Holder.BIG, 300);

		try {

			config.get(Holder.SMALL);

			fail();

		} catch(IllegalArgumentException e) {

			Assert.assertEquals("Cannot convert numeric value without loss! value=300 valueType=Integer targetType=Byte", e.getMessage());
		}

		config.add(Holder.BIG, 100);

		Assert.assertEquals(100, config.get(Holder.SMALL).byteValue());

		config.add(Holder.DOUBLE, 0.1d);

		try {

			config.get(Holder.FLOAT);

			fail();

		} catch(IllegalArgumentException e) {

			Assert.assertEquals("Cannot convert numeric value without loss! value=0.1 valueType=Double targetType=Float", e.getMessage());
		}

		config.add(Holder.DOUBLE, 0.5d);

		Assert.assertTrue(0.5f == config.get(Holder.FLOAT).floatValue());
	}
}
