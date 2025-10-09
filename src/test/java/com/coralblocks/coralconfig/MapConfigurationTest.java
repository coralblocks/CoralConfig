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

import com.coralblocks.coralconfig.ConfigKey.Kind;

public class MapConfigurationTest {
	
	static enum TestEnum {
		BALL, BOB, BILLY
	}
	
	public static final ConfigKey<Integer> TIMEOUT = ConfigKey.of("timeout", Integer.class, Kind.PRIMARY, null);
	
	public static final ConfigKey<Boolean> NO_REWIND = ConfigKey.of("noRewind", Boolean.class, Kind.PRIMARY, null);
	
	public static final ConfigKey<TestEnum> MY_ENUM = ConfigKey.of("myEnum", TestEnum.class, Kind.PRIMARY, null);
	
	@Test
	public void testBasics1() {
		
		Configuration config = new MapConfiguration(MapConfigurationTest.class, "myEnum=BALL noRewind=false");
		
		Assert.assertEquals(2, config.size());
		Assert.assertEquals(MapConfigurationTest.class, config.getHolder());
		Assert.assertEquals(2, config.keys().size());
		Assert.assertEquals(true, config.has(MY_ENUM));
		Assert.assertEquals(TestEnum.BALL, config.get(MY_ENUM));
		Assert.assertEquals(TestEnum.BALL, config.get(MY_ENUM, TestEnum.BILLY));
		Assert.assertEquals(TestEnum.BALL, config.get(MY_ENUM, TestEnum.BALL));
		Assert.assertEquals(false, config.has(TIMEOUT));
		Assert.assertEquals(false, config.get(NO_REWIND));
		Assert.assertEquals(false, config.get(NO_REWIND, true));
		Assert.assertEquals(false, config.get(NO_REWIND, false));
		Assert.assertEquals(1, config.get(TIMEOUT, 1).intValue());
		
		try {
			config.get(TIMEOUT);
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
	}
	
	@Test
	public void testBasics2() {
		
		MapConfiguration c = new MapConfiguration(MapConfigurationTest.class, "myEnum=BALL noRewind=false");
		
		Configuration config = new MapConfiguration(c);
		
		c.remove(MY_ENUM); // won't affect the new configuration
		
		Assert.assertEquals(2, config.size());
		Assert.assertEquals(MapConfigurationTest.class, config.getHolder());
		Assert.assertEquals(2, config.keys().size());
		Assert.assertEquals(true, config.has(MY_ENUM));
		Assert.assertEquals(TestEnum.BALL, config.get(MY_ENUM));
		Assert.assertEquals(TestEnum.BALL, config.get(MY_ENUM, TestEnum.BILLY));
		Assert.assertEquals(TestEnum.BALL, config.get(MY_ENUM, TestEnum.BALL));
		Assert.assertEquals(false, config.has(TIMEOUT));
		Assert.assertEquals(false, config.get(NO_REWIND));
		Assert.assertEquals(false, config.get(NO_REWIND, true));
		Assert.assertEquals(false, config.get(NO_REWIND, false));
		Assert.assertEquals(1, config.get(TIMEOUT, 1).intValue());
		
		try {
			config.get(TIMEOUT);
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
	}
	
	@Test
	public void testBasics3() {
		
		MapConfiguration config = new MapConfiguration(MapConfigurationTest.class, "myEnum=BALL noRewind=false");
		
		try {
			config.add(TIMEOUT, null); // null values are not allowed
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
		
		TestEnum prevEnum = config.add(MY_ENUM, TestEnum.BILLY);
		Assert.assertEquals(TestEnum.BALL, prevEnum);
		
		boolean prevNoRewind = config.remove(NO_REWIND);
		Assert.assertEquals(false, prevNoRewind);
		Assert.assertEquals(1, config.size());
		
		try {
			@SuppressWarnings("unused")
			int timeout = config.remove(TIMEOUT); // autoboxing NPE
			fail();
		} catch(NullPointerException e) {
			// Good
		}
		
		try {
			config.remove(TIMEOUT); // NO autoboxing NPE
		} catch(NullPointerException e) {
			fail();
		}
		
		Object prevTimeout = config.add(TIMEOUT, 33);
		Assert.assertEquals(null, prevTimeout);
		
		Assert.assertEquals(2, config.size());
		Assert.assertEquals(MapConfigurationTest.class, config.getHolder());
		Assert.assertEquals(2, config.keys().size());
		Assert.assertEquals(true, config.has(MY_ENUM));
		Assert.assertEquals(TestEnum.BILLY, config.get(MY_ENUM));
		Assert.assertEquals(TestEnum.BILLY, config.get(MY_ENUM, TestEnum.BILLY));
		Assert.assertEquals(TestEnum.BILLY, config.get(MY_ENUM, TestEnum.BALL));
		Assert.assertEquals(false, config.has(NO_REWIND));
		Assert.assertEquals(true, config.has(TIMEOUT));
		Assert.assertEquals(33, config.get(TIMEOUT).intValue());
		Assert.assertEquals(33, config.get(TIMEOUT, 77).intValue());
		Assert.assertEquals(33, config.get(TIMEOUT, 33).intValue());
		Assert.assertEquals(true, config.get(NO_REWIND, true));
		
		try {
			config.get(NO_REWIND);
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
	}
}