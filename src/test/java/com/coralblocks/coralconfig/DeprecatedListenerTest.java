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
		
		@Override
		public void deprecatedConfig(ConfigKey<?> deprecatedKey, ConfigKey<?> primaryKey) {
			calls++;
			deprecated = deprecatedKey;
			primary = primaryKey;
		}
	}
	
	@Test
	public void testListenerReportsEachDeprecatedKeyOnce() {
	
		class Base {
			
			public static final ConfigKey<Float> ANOTHER_FLOAT1 = floatKey(3f);
			public static final ConfigKey<Float> ANOTHER_FLOAT2 = floatKey(3f).alias(ANOTHER_FLOAT1);
			public static final ConfigKey<Float> ANOTHER_FLOAT3 = floatKey().def(3f).deprecated(ANOTHER_FLOAT1);
		}
		
		MapConfiguration config = new MapConfiguration(Base.class);
		
		TestListener testListener = new TestListener();
		
		config.addListener(testListener);
		
		config.has(Base.ANOTHER_FLOAT3);
		config.hasOverwrittenDefault(Base.ANOTHER_FLOAT3);
		config.getOverwrittenDefault(Base.ANOTHER_FLOAT3);
		config.remove(Base.ANOTHER_FLOAT3);
		config.removeOverwrittenDefault(Base.ANOTHER_FLOAT3);

		Assert.assertEquals(0, testListener.calls);

		config.get(Base.ANOTHER_FLOAT1);
		config.get(Base.ANOTHER_FLOAT2);
		config.get(Base.ANOTHER_FLOAT3);
		config.get(Base.ANOTHER_FLOAT3);
		
		Assert.assertEquals(1, testListener.calls);
		Assert.assertEquals(Base.ANOTHER_FLOAT3, testListener.deprecated);
		Assert.assertEquals(Base.ANOTHER_FLOAT1, testListener.primary);

		MapConfiguration addConfig = new MapConfiguration(Base.class);
		TestListener addListener = new TestListener();
		addConfig.addListener(addListener);
		addConfig.add(Base.ANOTHER_FLOAT3, 4f);
		addConfig.add(Base.ANOTHER_FLOAT3, 5f);

		Assert.assertEquals(1, addListener.calls);

		MapConfiguration defaultConfig = new MapConfiguration(Base.class);
		TestListener defaultListener = new TestListener();
		defaultConfig.addListener(defaultListener);
		defaultConfig.overwriteDefault(Base.ANOTHER_FLOAT3, 4f);
		defaultConfig.overwriteDefault(Base.ANOTHER_FLOAT3, 5f);

		Assert.assertEquals(1, defaultListener.calls);
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
		Assert.assertEquals(1, following.calls);
	}

	@Test
	public void testCopyDoesNotNotifyOrCopyListeners() {

		class Holder {

			public static final ConfigKey<Integer> PRIMARY = intKey(1);
			public static final ConfigKey<Integer> DEPRECATED = intKey(2).deprecated(PRIMARY);
		}

		MapConfiguration source = new MapConfiguration(Holder.class);
		source.add(Holder.DEPRECATED, 7);
		source.overwriteDefault(Holder.DEPRECATED, 8);

		TestListener sourceListener = new TestListener();
		source.addListener(sourceListener);

		MapConfiguration copy = new MapConfiguration(source);

		Assert.assertEquals(0, sourceListener.calls);
		Assert.assertEquals(7, copy.get(Holder.DEPRECATED).intValue());
		Assert.assertEquals(0, sourceListener.calls);

		copy.remove(Holder.DEPRECATED);
		Assert.assertEquals(8, copy.get(Holder.DEPRECATED).intValue());
		Assert.assertEquals(0, sourceListener.calls);

		TestListener copyListener = new TestListener();
		copy.addListener(copyListener);
		copy.get(Holder.DEPRECATED);
		copy.get(Holder.DEPRECATED);

		Assert.assertEquals(1, copyListener.calls);
		Assert.assertEquals(0, sourceListener.calls);
	}
}
