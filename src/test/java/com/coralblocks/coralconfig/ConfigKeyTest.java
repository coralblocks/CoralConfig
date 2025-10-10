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

public class ConfigKeyTest {
	
	static enum TestEnum {
		BLAH, FOO, CAT, DOG
	}
	
	@Test
	public void testBasics() {
		
		ConfigKey<Integer> intKey = ConfigKey.of(Integer.class, Kind.PRIMARY, null);
		
		Integer value = intKey.parseValue("3");
		
		Assert.assertEquals(3, value.intValue());
		Assert.assertEquals(null, intKey.getFieldName());
		
		ConfigKey<TestEnum> enumKey = ConfigKey.of(TestEnum.class, Kind.PRIMARY, null);
		
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
}