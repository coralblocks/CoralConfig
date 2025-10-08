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


public class ConfigTest {
	
	@Test
	public void testBasics() {
		
		class Base1 {
			
			public static final ConfigKey<Integer> TIMEOUT = ConfigKey.of("timeout", Integer.class, Kind.PRIMARY, null);
			
			public static final ConfigKey<Boolean> NO_REWIND = ConfigKey.of("noRewind", Boolean.class, Kind.PRIMARY, null);
		}
		
		Config config1 = Config.of(Base1.class);
		
		Assert.assertEquals(2, config1.size());
		Assert.assertEquals(Base1.TIMEOUT, config1.get("timeout"));
		Assert.assertEquals(Base1.NO_REWIND, config1.get("noRewind"));
		
		@SuppressWarnings("unused")
		class Base2 {
			
			public static final ConfigKey<Integer> TIMEOUT1 = ConfigKey.of("timeout", Integer.class, Kind.PRIMARY, null);
			
			public static final ConfigKey<Boolean> TIMEOUT2 = ConfigKey.of("timeout", Boolean.class, Kind.PRIMARY, null);
		}
		
		try {
		
			Config.of(Base2.class);
			
			fail();
			
		} catch(IllegalStateException e) {
			// Must throw exception
		}
	}
}