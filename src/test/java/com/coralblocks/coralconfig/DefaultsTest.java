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


public class DefaultsTest {
	
	@Test
	public void testNullDefaults() {
		
		class Base1 {
			public static final ConfigKey<String> MY_STRING = stringKey().def(null); 
		}
		
		MapConfiguration mc1 = new MapConfiguration(Base1.class);
		Assert.assertEquals(false, Base1.MY_STRING.isRequired());
		Assert.assertNull(mc1.get(Base1.MY_STRING));
		
		class Base2 {
			public static final ConfigKey<String> MY_STRING = stringKey();
		}
		
		MapConfiguration mc2 = new MapConfiguration(Base2.class);
		Assert.assertEquals(true, Base2.MY_STRING.isRequired());
		
		try {
			mc2.get(Base2.MY_STRING); // REQUIRED
			fail();
		} catch(RuntimeException e) {
			// Good!
		}
	}
	
	@Test
	public void testBasicsOfBasics() {
		
		class Base {
		    public static final ConfigKey<Integer> HEARTBEAT_INTERVAL = intKey().def(5);
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
		
		Assert.assertEquals(TestEnum.BOB, mc.get(Base.MY_ENUM)); // test default
		
		mc.overwriteDefault(Base.MY_ENUM, TestEnum.BILL);
		
		Assert.assertEquals(TestEnum.BILL, mc.get(Base.MY_ENUM)); // test overwritten default
		
		mc.add(Base.MY_ENUM, TestEnum.BALL);
		
		Assert.assertEquals(TestEnum.BALL, mc.get(Base.MY_ENUM)); // test configured
		
		mc.remove(Base.MY_ENUM);
		
		Assert.assertEquals(TestEnum.BILL, mc.get(Base.MY_ENUM)); // test overwritten default
		
		mc.overwriteDefault(Base.MY_ENUM, null); // String and Enum support NULL !!!
		
		Assert.assertEquals(null, mc.get(Base.MY_ENUM)); // test overwritten default
		
		mc.removeOverwrittenDefault(Base.MY_ENUM);
		
		Assert.assertEquals(TestEnum.BOB, mc.get(Base.MY_ENUM)); // test default
	}
	
	@Test
	public void testEnumNullDefault() {
		
		enum TestEnum { BALL, BOB, BILL }
		
		class Base {
		    public static final ConfigKey<TestEnum> MY_ENUM = enumKey(TestEnum.class).def(null);
		}
		
		MapConfiguration mc = new MapConfiguration(Base.class);
		
		Assert.assertEquals(null, mc.get(Base.MY_ENUM)); // test default
		
		mc.overwriteDefault(Base.MY_ENUM, TestEnum.BILL);
		
		Assert.assertEquals(TestEnum.BILL, mc.get(Base.MY_ENUM)); // test overwritten default
		
		mc.add(Base.MY_ENUM, TestEnum.BALL);
		
		Assert.assertEquals(TestEnum.BALL, mc.get(Base.MY_ENUM)); // test configured
		
		mc.remove(Base.MY_ENUM);
		
		Assert.assertEquals(TestEnum.BILL, mc.get(Base.MY_ENUM)); // test overwritten default
		
		mc.overwriteDefault(Base.MY_ENUM, null); // String and Enum support NULL !!!
		
		Assert.assertEquals(null, mc.get(Base.MY_ENUM)); // test overwritten default
		
		mc.removeOverwrittenDefault(Base.MY_ENUM);
		
		Assert.assertEquals(null, mc.get(Base.MY_ENUM)); // test default
	}
	
	@Test
	public void testDefaults1() {
		
		class Holder {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey().def(10);
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().def(20).deprecated(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_3 = intKey().def(30).alias(MY_INTEGER_1);
		}
		
		MapConfiguration mc1 = new MapConfiguration(Holder.class);
		
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 11);
		
		Assert.assertEquals(11, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(11, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(11, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		
		Assert.assertEquals(10, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(20, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(30, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 22);
		
		Assert.assertEquals(22, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(22, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(22, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 33);
		
		Assert.assertEquals(33, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(33, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(33, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 88);
		
		Assert.assertEquals(66, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(77, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(88, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		
		Assert.assertEquals(66, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(77, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(66, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(66, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(66, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(77, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		// REMEMBER: Alias has precedence over deprecated!
		
		Assert.assertEquals(77, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(66, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(77, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 66);
		
		// REMEMBER: Alias has precedence over deprecated!
		
		Assert.assertEquals(77, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(66, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(77, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		
		Assert.assertEquals(66, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(66, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(77, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(77, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(77, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(77, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		
		Assert.assertEquals(77, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(77, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(77, mc1.get(Holder.MY_INTEGER_3).intValue());
	}
	
	@Test
	public void testDefaults2() {
		
		class Holder {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey().def(10);
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().def(20).deprecated(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_3 = intKey().def(30).alias(MY_INTEGER_1);
		}
		
		MapConfiguration mc1 = new MapConfiguration("myInteger1=5", Holder.class);
		
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 11);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 22);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 33);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 88);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 66);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
	}
	
	@Test
	public void testDefaults3() {
		
		class Holder {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey().def(10);
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().def(20).deprecated(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_3 = intKey().def(30).alias(MY_INTEGER_1);
		}
		
		MapConfiguration mc1 = new MapConfiguration("myInteger2=5", Holder.class);
		
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 11);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 22);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 33);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 88);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 66);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
	}
	
	@Test
	public void testDefaults4() {
		
		class Holder {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey().def(10);
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().def(20).deprecated(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_3 = intKey().def(30).alias(MY_INTEGER_1);
		}
		
		MapConfiguration mc1 = new MapConfiguration("myInteger3=5", Holder.class);
		
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 11);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 22);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 33);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 88);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 66);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
	}
	
	@Test
	public void testDefaults5() {
		
		class Holder {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey().def(10);
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().def(20).deprecated(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_3 = intKey().def(30).alias(MY_INTEGER_1);
		}
		
		MapConfiguration mc1 = new MapConfiguration("myInteger2=6 myInteger3=5", Holder.class);
		
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 11);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 22);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 33);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 88);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 66);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
	}
	
	@Test
	public void testDefaults6() {
		
		class Holder {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey().def(10);
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().def(20).deprecated(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_3 = intKey().def(30).alias(MY_INTEGER_1);
		}
		
		MapConfiguration mc1 = new MapConfiguration("myInteger1=5 myInteger3=6", Holder.class);
		
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 11);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 22);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 33);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 88);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 66);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_3).intValue());
	}
	
	@Test
	public void testDefaults7() {
		
		class Holder {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey().def(10);
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().def(20).deprecated(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_3 = intKey().def(30).alias(MY_INTEGER_1);
		}
		
		MapConfiguration mc1 = new MapConfiguration("myInteger1=5 myInteger2=6", Holder.class);
		
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 11);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 22);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 33);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 88);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 66);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 66);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		mc1.overwriteDefault(Holder.MY_INTEGER_1, 66);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_3, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_1);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_2);
		mc1.removeOverwrittenDefault(Holder.MY_INTEGER_3);
		mc1.overwriteDefault(Holder.MY_INTEGER_2, 77);
		
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(6, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc1.get(Holder.MY_INTEGER_3).intValue());
	}
}