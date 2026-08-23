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

import org.junit.Assert;
import org.junit.Test;


public class DeprecatedListenerTest {
	
	static class TestListener implements DeprecatedListener {
		
		int calls;
		ConfigKey<?> deprecated;
		ConfigKey<?> primary;
		
		void reset() {
			calls = 0;
			deprecated = null;
			primary = null;
		}
		
		@Override
		public void deprecatedConfig(ConfigKey<?> deprecatedKey, ConfigKey<?> primaryKey) {
			calls++;
			deprecated = deprecatedKey;
			primary = primaryKey;
		}
	}
	
	@Test
	public void testListener() {
	
		class Base {
			
			public static final ConfigKey<Float> ANOTHER_FLOAT1 = floatKey(3f);
			public static final ConfigKey<Float> ANOTHER_FLOAT2 = floatKey(3f).alias(ANOTHER_FLOAT1);
			public static final ConfigKey<Float> ANOTHER_FLOAT3 = floatKey().def(3f).deprecated(ANOTHER_FLOAT1);
		}
		
		MapConfiguration config = new MapConfiguration(Base.class);
		
		TestListener testListener = new TestListener();
		
		config.addListener(testListener);
		
		config.get(Base.ANOTHER_FLOAT1);
		config.get(Base.ANOTHER_FLOAT2);
		config.get(Base.ANOTHER_FLOAT3);
		
		Assert.assertEquals(1, testListener.calls);
		Assert.assertEquals(Base.ANOTHER_FLOAT3, testListener.deprecated);
		Assert.assertEquals(Base.ANOTHER_FLOAT1, testListener.primary);
		
		testListener.reset();
		
		config.add(Base.ANOTHER_FLOAT1, 3f);
		config.add(Base.ANOTHER_FLOAT2, 3f);
		config.add(Base.ANOTHER_FLOAT3, 3f);
		
		Assert.assertEquals(1, testListener.calls);
		Assert.assertEquals(Base.ANOTHER_FLOAT3, testListener.deprecated);
		Assert.assertEquals(Base.ANOTHER_FLOAT1, testListener.primary);
		
		testListener.reset();
		
		config.remove(Base.ANOTHER_FLOAT1);
		config.remove(Base.ANOTHER_FLOAT2);
		config.remove(Base.ANOTHER_FLOAT3);
		
		Assert.assertEquals(1, testListener.calls);
		Assert.assertEquals(Base.ANOTHER_FLOAT3, testListener.deprecated);
		Assert.assertEquals(Base.ANOTHER_FLOAT1, testListener.primary);
		
		testListener.reset();
		
		config.overwriteDefault(Base.ANOTHER_FLOAT1, 3f);
		config.overwriteDefault(Base.ANOTHER_FLOAT2, 3f);
		config.overwriteDefault(Base.ANOTHER_FLOAT3, 3f);
		
		Assert.assertEquals(1, testListener.calls);
		Assert.assertEquals(Base.ANOTHER_FLOAT3, testListener.deprecated);
		Assert.assertEquals(Base.ANOTHER_FLOAT1, testListener.primary);
		
		testListener.reset();
		
		config.getOverwrittenDefault(Base.ANOTHER_FLOAT1);
		config.getOverwrittenDefault(Base.ANOTHER_FLOAT2);
		config.getOverwrittenDefault(Base.ANOTHER_FLOAT3);
		
		Assert.assertEquals(1, testListener.calls);
		Assert.assertEquals(Base.ANOTHER_FLOAT3, testListener.deprecated);
		Assert.assertEquals(Base.ANOTHER_FLOAT1, testListener.primary);
	}

	@Test
	public void testListenerCanRemoveItself() {

		class Holder {

			public static final ConfigKey<Integer> PRIMARY = intKey(1);
			public static final ConfigKey<Integer> DEPRECATED = intKey().deprecated(PRIMARY);
		}

		MapConfiguration config = new MapConfiguration(Holder.class);
		int[] selfRemovingCalls = new int[1];

		DeprecatedListener selfRemoving = new DeprecatedListener() {

			@Override
			public void deprecatedConfig(ConfigKey<?> deprecatedKey, ConfigKey<?> primaryKey) {
				selfRemovingCalls[0]++;
				config.removeListener(this);
			}
		};

		TestListener following = new TestListener();
		config.addListener(selfRemoving);
		config.addListener(following);

		config.get(Holder.DEPRECATED);

		Assert.assertEquals(1, selfRemovingCalls[0]);
		Assert.assertEquals(1, following.calls);

		config.get(Holder.DEPRECATED);

		Assert.assertEquals(1, selfRemovingCalls[0]);
		Assert.assertEquals(2, following.calls);
	}
}
