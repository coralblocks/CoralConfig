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

import static com.coralblocks.coralconfig.ConfigKey.*;

public class ConfigKeyHelpersTest {
	
	static enum TestEnum {
		BALL, BOB, BILLY
	}
	
	public static final ConfigKey<Integer> TIMEOUT = intKey();
	
	public static final ConfigKey<Boolean> NO_REWIND = boolKey();
	
	public static final ConfigKey<TestEnum> MY_ENUM = enumKey(TestEnum.class);
	
	@Test
	public void testBasics1() {
		
		Configuration config = new MapConfiguration("myEnum=BALL noRewind=false", ConfigKeyHelpersTest.class);
		
		Assert.assertEquals(2, config.size());
		Assert.assertEquals(ConfigKeyHelpersTest.class, config.getHolders()[0]);
		Assert.assertEquals(2, config.keys().size());
		Assert.assertEquals(true, config.has(MY_ENUM));
		Assert.assertEquals(TestEnum.BALL, config.get(MY_ENUM));
		Assert.assertEquals(false, config.has(TIMEOUT));
		Assert.assertEquals(false, config.get(NO_REWIND));
		
		try {
			config.get(TIMEOUT);
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
	}
	
	@Test
	public void testBasics2() {
		
		MapConfiguration mc = new MapConfiguration("myEnum=BALL noRewind=false", ConfigKeyHelpersTest.class);
		
		mc.overwriteDefault(MY_ENUM, null);
		mc.overwriteDefault(TIMEOUT, 10);
		
		MapConfiguration mapConfig = new MapConfiguration(mc);
		
		mc.remove(MY_ENUM); // won't affect the new configuration
		
		Assert.assertEquals(2, mapConfig.size());
		Assert.assertEquals(ConfigKeyHelpersTest.class, mapConfig.getHolders()[0]);
		Assert.assertEquals(2, mapConfig.keys().size());
		Assert.assertEquals(true, mapConfig.has(MY_ENUM));
		Assert.assertEquals(TestEnum.BALL, mapConfig.get(MY_ENUM));
		Assert.assertEquals(false, mapConfig.has(TIMEOUT));
		Assert.assertEquals(false, mapConfig.get(NO_REWIND));
		
		try {
			mapConfig.get(TIMEOUT);
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
		
		mapConfig.remove(MY_ENUM);
		
		try {
			mapConfig.get(MY_ENUM);
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
	}
	
	@Test
	public void testBasics3() {
		
		MapConfiguration mapConfig = new MapConfiguration("myEnum=BALL noRewind=false", ConfigKeyHelpersTest.class);
		
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
		Assert.assertEquals(ConfigKeyHelpersTest.class, mapConfig.getHolders()[0]);
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
}