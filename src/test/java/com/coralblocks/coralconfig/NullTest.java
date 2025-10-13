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

import static com.coralblocks.coralconfig.ConfigKey.*;
import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

public class NullTest {
	
	static enum TestEnum {
		BALL, BOB, BILLY
	}
	
	public static final ConfigKey<Integer> MY_INTEGER = intKey();
	
	public static final ConfigKey<TestEnum> MY_ENUM = enumKey(TestEnum.class).def(TestEnum.BOB);
	
	@Test
	public void testNullEnum() {
		
		MapConfiguration mc1 = new MapConfiguration(NullTest.class);
		
		Assert.assertEquals(TestEnum.BOB, mc1.get(MY_ENUM));
		
		mc1.overwriteDefault(MY_ENUM, TestEnum.BALL);
		
		Assert.assertEquals(TestEnum.BALL, mc1.get(MY_ENUM));
		
		mc1.overwriteDefault(MY_ENUM, null);
		
		Assert.assertEquals(null, mc1.get(MY_ENUM));
		
		mc1.removeOverwrittenDefault(MY_ENUM);
		
		Assert.assertEquals(TestEnum.BOB, mc1.get(MY_ENUM));
		
		mc1.add(MY_ENUM, TestEnum.BILLY);
		
		Assert.assertEquals(TestEnum.BILLY, mc1.get(MY_ENUM));
		
		mc1.overwriteDefault(MY_ENUM, TestEnum.BALL);
		
		Assert.assertEquals(TestEnum.BILLY, mc1.get(MY_ENUM));
		
		mc1.overwriteDefault(MY_ENUM, null);
		
		Assert.assertEquals(TestEnum.BILLY, mc1.get(MY_ENUM));
		
		mc1.remove(MY_ENUM);
		
		Assert.assertEquals(null, mc1.get(MY_ENUM));
		
		mc1.overwriteDefault(MY_ENUM, TestEnum.BALL);
		
		Assert.assertEquals(TestEnum.BALL, mc1.get(MY_ENUM));
	}
	
	@Test
	public void testNullInteger() {
		
		// If no default for the config key was defined, then the config key is mandatory, in other words, 
		// it must be present in the configuration
		// Attention: Overwriting a default that does not exist won't dot it in this case, the config key is really mandatory.
		
		MapConfiguration mc1 = new MapConfiguration(NullTest.class);
		
		try {
			mc1.overwriteDefault(MY_INTEGER, null);
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
		
		try {
			Assert.assertEquals(1, mc1.get(MY_INTEGER).intValue());
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
		
		try {
			mc1.overwriteDefault(MY_INTEGER, 1);	
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
	}
	
	@Test
	public void testDefaultsToRemember() {

		class Holder1 {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey();
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().def(10).deprecated(MY_INTEGER_1);
			
		}
		
		MapConfiguration mc1 = new MapConfiguration(Holder1.class);
		
		// Question: Does the primary config key get the default from the deprecated config key?
		
		Assert.assertEquals(10, mc1.get(Holder1.MY_INTEGER_1).intValue());
		
		// Answer: YES!
		
		// Question: Now what happens if we try to overwrite the default of the primary key?
		
		mc1.overwriteDefault(Holder1.MY_INTEGER_1, 11);
		
		// Answer: It works because the primary config key is returning a default value!
		
		Assert.assertEquals(11, mc1.get(Holder1.MY_INTEGER_1).intValue()); // It returns a default, so it can be overwritten!

		// Question: What happens if we overwrite the deprecated one and not the primary one?
		
		mc1.removeOverwrittenDefault(Holder1.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder1.MY_INTEGER_2);
		mc1.overwriteDefault(Holder1.MY_INTEGER_2, 11);
		
		Assert.assertEquals(11, mc1.get(Holder1.MY_INTEGER_1).intValue());
		
		// Answer: It still works fine!
		
		// Question: Now what happens if we overwrite both with the same value?
		
		mc1.removeOverwrittenDefault(Holder1.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder1.MY_INTEGER_2);
		mc1.overwriteDefault(Holder1.MY_INTEGER_1, 11);
		mc1.overwriteDefault(Holder1.MY_INTEGER_2, 11);
		
		Assert.assertEquals(11, mc1.get(Holder1.MY_INTEGER_1).intValue());
		
		// Answer: It works fine as expected!
		
		// Question: Now what happens if we overwrite both with the different values?
		
		mc1.removeOverwrittenDefault(Holder1.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder1.MY_INTEGER_2);
		mc1.overwriteDefault(Holder1.MY_INTEGER_1, 11);
		mc1.overwriteDefault(Holder1.MY_INTEGER_2, 22);
		
		Assert.assertEquals(11, mc1.get(Holder1.MY_INTEGER_1).intValue());
		
		// Answer: Of course it will honor its own overwritten value!

		@SuppressWarnings("unused")
		class Holder11 {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey();
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().def(10).deprecated(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_3 = intKey().def(20).deprecated(MY_INTEGER_1);
			
		}
		
		MapConfiguration mc11 = new MapConfiguration(Holder11.class);
		
		// Question: Does the primary config key get the default from the deprecated config key?
		
		try {
			Assert.assertEquals(10, mc11.get(Holder11.MY_INTEGER_1).intValue());
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
		
		// Answer: No because there are two values! (ambiguous)
		
		// Question: Now what happens if we try to overwrite the default of the primary key?
		
		try {
			mc11.overwriteDefault(Holder11.MY_INTEGER_1, 11);
			fail();
		} catch(IllegalStateException e) {
			// Good!
		}
		
		// Answer: It does not work, because a default value cannot be returned!
		
		@SuppressWarnings("unused")
		class Holder2 {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey();
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().def(20).alias(MY_INTEGER_1);
			
		}
		
		MapConfiguration mc2 = new MapConfiguration(Holder2.class);
		
		// Question: Does the primary config key get the default from the alias config key?
		
		Assert.assertEquals(20, mc2.get(Holder2.MY_INTEGER_1).intValue());
		
		// Answer: YES!
		
		@SuppressWarnings("unused")
		class Holder3 {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey();
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().def(11).deprecated(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_3 = intKey().def(22).alias(MY_INTEGER_1);
			
		}
		
		MapConfiguration mc3 = new MapConfiguration(Holder3.class);
		
		// Question: Now what happens when both alias and deprecated are defined with different defaults?
		
		try {
			Assert.assertEquals(22, mc3.get(Holder3.MY_INTEGER_1).intValue());
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
		
		// Answer: An exception is thrown => It does NOT work!
		
		@SuppressWarnings("unused")
		class Holder4 {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey();
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().def(22).deprecated(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_3 = intKey().def(22).alias(MY_INTEGER_1);
			
		}
		
		MapConfiguration mc4 = new MapConfiguration(Holder4.class);
		
		// Question: Now what happens when both alias and deprecated are defined with the same default?
		
		Assert.assertEquals(22, mc4.get(Holder4.MY_INTEGER_1).intValue());
		
		// Answer: It works and the default is returned!
		
		class Holder5 {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey();
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().deprecated(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_3 = intKey().def(22).alias(MY_INTEGER_1);
			
		}
		
		MapConfiguration mc5 = new MapConfiguration(Holder5.class);
		
		// Question: Now what happens when deprecated does not have default and alias has a default?
		
		Assert.assertEquals(22, mc5.get(Holder5.MY_INTEGER_1).intValue()); // It works!
		Assert.assertEquals(22, mc5.get(Holder5.MY_INTEGER_3).intValue()); // Of course it works!
		try {
			Assert.assertEquals(22, mc5.get(Holder5.MY_INTEGER_2).intValue()); // This does not work!
			fail();
		} catch(RuntimeException e) { /* Good! */ }
		
		// Answer: It works and the default is returned!
		
		class Holder6 {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey();
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().def(22).deprecated(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_3 = intKey().alias(MY_INTEGER_1);
			
		}
		
		MapConfiguration mc6 = new MapConfiguration(Holder6.class);
		
		// Question: Now what happens when alias does not have default and deprecated has a default?
		
		Assert.assertEquals(22, mc6.get(Holder6.MY_INTEGER_1).intValue()); // It works!
		Assert.assertEquals(22, mc6.get(Holder6.MY_INTEGER_2).intValue()); // Of course it works!
		try {
			Assert.assertEquals(22, mc6.get(Holder6.MY_INTEGER_3).intValue()); // This does not work!
			fail();
		} catch(RuntimeException e) { /* Good! */ }
		
		// Answer: It works and the default is returned!
	}
}
	