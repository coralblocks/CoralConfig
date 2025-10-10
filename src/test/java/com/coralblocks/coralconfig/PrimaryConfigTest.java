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

public class PrimaryConfigTest {

	@Test
	public void testNoPrimaryKeys() {
		
		@SuppressWarnings("unused")
		class Base1 {
			
			enum TestEnum {
				BALL, BOB, BILLY
			}
			
			public static final ConfigKey<Integer> TIMEOUT = ConfigKey.intKey();
			
			public static final ConfigKey<Boolean> NO_REWIND = ConfigKey.boolKey();
			
			public static final ConfigKey<TestEnum> MY_ENUM = ConfigKey.enumKey(TestEnum.class);
			
			public static final ConfigKey<Boolean> NO_REWIND1 = ConfigKey.boolKeyAlias(NO_REWIND);
			
			public static final ConfigKey<Boolean> NO_REWIND2 = ConfigKey.boolKeyAlias(NO_REWIND);

			public static final ConfigKey<Float> NEW_FLOAT = ConfigKey.floatKey();
			public static final ConfigKey<Float> INITIAL_FLOAT = ConfigKey.floatKeyDeprecated(NEW_FLOAT);
		}
		
		ConfigContainer cc = ConfigContainer.of(Base1.class);
		Assert.assertEquals(7, cc.size());
		
		try {
			ConfigKey.floatKeyDeprecated(null);
			fail();
		} catch(IllegalStateException e) {
			// Good!
		}
		
		@SuppressWarnings("unused")
		class Base2 {
			public static final ConfigKey<Float> ANOTHER_FLOAT1 = ConfigKey.floatKey();
			public static final ConfigKey<Float> ANOTHER_FLOAT2 = ConfigKey.floatKeyAlias(ANOTHER_FLOAT1);
			public static final ConfigKey<Float> ANOTHER_FLOAT3 = ConfigKey.floatKeyAlias(Base1.NEW_FLOAT);
		}
		
		try {
			cc = ConfigContainer.of(Base2.class);
			fail();
		} catch(IllegalStateException e) {
			// Good!
		}
		
		@SuppressWarnings("unused")
		class Base3 {
			public static final ConfigKey<Float> ANOTHER_FLOAT1 = ConfigKey.floatKey();
			public static final ConfigKey<Float> ANOTHER_FLOAT2 = ConfigKey.floatKeyAlias(ANOTHER_FLOAT1);
			public static final ConfigKey<Float> ANOTHER_FLOAT3 = ConfigKey.floatKeyAlias(Base2.ANOTHER_FLOAT1);
		}
		
		try {
			cc = ConfigContainer.of(Base3.class);
			fail();
		} catch(IllegalStateException e) {
			// Good!
		}
	}
}