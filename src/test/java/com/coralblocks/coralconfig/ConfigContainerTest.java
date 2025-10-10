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


public class ConfigContainerTest {
	
	@Test
	public void testBasics() {
		
		class Base1 {
			
			public static final ConfigKey<Integer> TIMEOUT = ConfigKey.of(Integer.class, Kind.PRIMARY, null);
			
			public static final ConfigKey<Boolean> NO_REWIND = ConfigKey.of(Boolean.class, Kind.PRIMARY, null);
		}
		
		class Blah {
		
			public static final ConfigKey<Integer> TIMEOUT = ConfigKey.of(Integer.class, Kind.PRIMARY, null);
		}
		
		ConfigContainer cc1 = ConfigContainer.of(Base1.class);
		
		Assert.assertEquals(2, cc1.size());
		Assert.assertEquals(Base1.TIMEOUT, cc1.get("timeout"));
		Assert.assertEquals(Base1.NO_REWIND, cc1.get("noRewind"));
		Assert.assertEquals(true, cc1.has(Base1.TIMEOUT));
		Assert.assertEquals(false, cc1.has(Blah.TIMEOUT));
		Assert.assertEquals("TIMEOUT", Base1.TIMEOUT.getFieldName());
		Assert.assertEquals("NO_REWIND", Base1.NO_REWIND.getFieldName());
		Assert.assertEquals(null, Blah.TIMEOUT.getFieldName()); // was not added to any Config
		
		@SuppressWarnings("unused")
		class Base2 {
			
			public static final ConfigKey<Integer> TIMEOUT = ConfigKey.of(Integer.class, Kind.PRIMARY, null);
			
		}
		
		@SuppressWarnings("unused")
		class Base3 {
			
			public static final ConfigKey<Boolean> TIMEOUT = ConfigKey.of(Boolean.class, Kind.PRIMARY, null);
		}
		
		try {
		
			ConfigContainer configContainer1 = ConfigContainer.of(Base2.class);
			ConfigContainer configContainer2 = ConfigContainer.of(Base3.class);
			
			ConfigContainer.enforceNoDuplicates(configContainer1, configContainer2);
			
			fail();
			
		} catch(IllegalStateException e) {
			// Good!
		}
	}
}