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


public class FluentApiTest {
	
	@Test
	public void testAllKids() {
		
		class Holder {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey().def(10);
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().def(20).deprecated(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_3 = intKey().def(30).alias(MY_INTEGER_1);
		}
		
		MapConfiguration mc1 = new MapConfiguration(Holder.class);
		
		// Nothing is defined in the configuration, so the respective default values apply!
		
		Assert.assertEquals(10, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(20, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(30, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		// Now the primary config key is defined in the configuration, so everything (primary, aliases and deprecated) gets it.
		
		MapConfiguration mc2 = new MapConfiguration("myInteger1=5", Holder.class);
		
		Assert.assertEquals(5, mc2.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc2.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc2.get(Holder.MY_INTEGER_3).intValue());
		
		// Now the deprecated config key is defined, so the primary gets it since there is no config key defined for the primary key.
		// The alias key should also get it, because the deprecated value defined relates to its primary config key.
		
		MapConfiguration mc3 = new MapConfiguration("myInteger2=15", Holder.class);
		
		Assert.assertEquals(15, mc3.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(15, mc3.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(15, mc3.get(Holder.MY_INTEGER_3).intValue());
		
		// Now the alias config key is defined, so the primary gets it since there is no config key defined for the primary key.
		// The deprecated key should also get it, because the alias value defined relates to its primary config key.
		
		MapConfiguration mc4 = new MapConfiguration("myInteger3=25", Holder.class);
		
		Assert.assertEquals(25, mc4.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(25, mc4.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(25, mc4.get(Holder.MY_INTEGER_3).intValue());
		
		// If a config key is defined, it gets its value normally
		// Here the alias is not defined but its primary key is defined => therefore its value is the value of its defined primary key
		
		MapConfiguration mc5 = new MapConfiguration("myInteger1=33 myInteger2=77", Holder.class);
		
		Assert.assertEquals(33, mc5.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(77, mc5.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(33, mc5.get(Holder.MY_INTEGER_3).intValue());
		
		// If a config key is defined, it gets its value normally
		// Here the alias is not defined but its primary key is defined => therefore its value is the value of its defined primary key
		
		MapConfiguration mc6 = new MapConfiguration("myInteger1=33 myInteger3=99", Holder.class);
		
		Assert.assertEquals(33, mc6.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(33, mc6.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(99, mc6.get(Holder.MY_INTEGER_3).intValue());
		
		// Only the alias and the deprecated are defined but the primary key is not defined
		// The primary config key gets its value from the alias. The alias always has precedence over deprecated config keys
		
		MapConfiguration mc7 = new MapConfiguration("myInteger2=66 myInteger3=88", Holder.class);
		
		Assert.assertEquals(88, mc7.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(66, mc7.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(88, mc7.get(Holder.MY_INTEGER_3).intValue());
		
		// If everything is defined, everything get its defined value
		
		MapConfiguration mc8 = new MapConfiguration("myInteger1=6 myInteger2=7 myInteger3=8", Holder.class);
		
		Assert.assertEquals(6, mc8.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(7, mc8.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(8, mc8.get(Holder.MY_INTEGER_3).intValue());
	}
	
	@Test
	public void testMultipleDeprecated() {
		
		class Holder {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey().def(10);
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().def(20).deprecated(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_3 = intKey().def(30).deprecated(MY_INTEGER_1);
		}
		
		MapConfiguration mc1 = new MapConfiguration(Holder.class);
		
		// Nothing is defined in the configuration, so the respective default values apply!
		
		Assert.assertEquals(10, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(20, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(30, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		// Now the primary config key is defined in the configuration, so both deprecated config keys get it.
		
		MapConfiguration mc2 = new MapConfiguration("myInteger1=5", Holder.class);
		
		Assert.assertEquals(5, mc2.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc2.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc2.get(Holder.MY_INTEGER_3).intValue());
		
		// Now the first deprecated config key is defined, so the primary gets it since there is no config key defined for the primary key.
		// The other deprecated key should also get it, because the deprecated value defined relates to its primary config key.
		
		MapConfiguration mc3 = new MapConfiguration("myInteger2=15", Holder.class);
		
		Assert.assertEquals(15, mc3.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(15, mc3.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(15, mc3.get(Holder.MY_INTEGER_3).intValue());

		// Same as above (testing the other deprecated config key)
		
		MapConfiguration mc4 = new MapConfiguration("myInteger3=25", Holder.class);
		
		Assert.assertEquals(25, mc4.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(25, mc4.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(25, mc4.get(Holder.MY_INTEGER_3).intValue());
		
		// If a config key is defined, it gets its value normally
		// Here the last deprecated is not defined but its primary key is defined => therefore its value is the value of its defined primary key
		
		MapConfiguration mc5 = new MapConfiguration("myInteger1=33 myInteger2=77", Holder.class);
		
		Assert.assertEquals(33, mc5.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(77, mc5.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(33, mc5.get(Holder.MY_INTEGER_3).intValue());
		
		// If a config key is defined, it gets its value normally
		// Here the first deprecated key is not defined but its primary key is defined => therefore its value is the value of its defined primary key
		
		MapConfiguration mc6 = new MapConfiguration("myInteger1=33 myInteger3=99", Holder.class);
		
		Assert.assertEquals(33, mc6.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(33, mc6.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(99, mc6.get(Holder.MY_INTEGER_3).intValue());
		
		// Here we have both deprecated config keys defined, so from which one does the primary config key get its value?
		// From the very first one that is defined after it
		
		MapConfiguration mc7 = new MapConfiguration("myInteger2=66 myInteger3=88", Holder.class);
		
		Assert.assertEquals(66, mc7.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(66, mc7.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(88, mc7.get(Holder.MY_INTEGER_3).intValue());
		
		// If everything is defined, everything get its defined value
		
		MapConfiguration mc8 = new MapConfiguration("myInteger1=6 myInteger2=7 myInteger3=8", Holder.class);
		
		Assert.assertEquals(6, mc8.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(7, mc8.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(8, mc8.get(Holder.MY_INTEGER_3).intValue());
	}
	
	@Test
	public void testMultipleAliases() {
		
		class Holder {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey().def(10);
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().def(20).alias(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_3 = intKey().def(30).alias(MY_INTEGER_1);
		}
		
		MapConfiguration mc1 = new MapConfiguration(Holder.class);
		
		// Nothing is defined in the configuration, so the respective default values apply!
		
		Assert.assertEquals(10, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(20, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(30, mc1.get(Holder.MY_INTEGER_3).intValue());
		
		// Now the primary config key is defined in the configuration, so both aliases config keys get it.
		
		MapConfiguration mc2 = new MapConfiguration("myInteger1=5", Holder.class);
		
		Assert.assertEquals(5, mc2.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc2.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc2.get(Holder.MY_INTEGER_3).intValue());
		
		// Now the first alias config key is defined, so the primary gets it since there is no config key defined for the primary key.
		// The other alias key should also get it, because the deprecated value defined relates to its primary config key.
		
		MapConfiguration mc3 = new MapConfiguration("myInteger2=15", Holder.class);
		
		Assert.assertEquals(15, mc3.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(15, mc3.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(15, mc3.get(Holder.MY_INTEGER_3).intValue());

		// Same as above (testing the other alias config key)
		
		MapConfiguration mc4 = new MapConfiguration("myInteger3=25", Holder.class);
		
		Assert.assertEquals(25, mc4.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(25, mc4.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(25, mc4.get(Holder.MY_INTEGER_3).intValue());
		
		// If a config key is defined, it gets its value normally
		// Here the last alias is not defined but its primary key is defined => therefore its value is the value of its defined primary key
		
		MapConfiguration mc5 = new MapConfiguration("myInteger1=33 myInteger2=77", Holder.class);
		
		Assert.assertEquals(33, mc5.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(77, mc5.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(33, mc5.get(Holder.MY_INTEGER_3).intValue());
		
		// If a config key is defined, it gets its value normally
		// Here the first alias key is not defined but its primary key is defined => therefore its value is the value of its defined primary key
		
		MapConfiguration mc6 = new MapConfiguration("myInteger1=33 myInteger3=99", Holder.class);
		
		Assert.assertEquals(33, mc6.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(33, mc6.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(99, mc6.get(Holder.MY_INTEGER_3).intValue());
		
		// Here we have both aliases config keys defined, so from which one does the primary config key get its value?
		// From the very first one that is defined after it
		
		MapConfiguration mc7 = new MapConfiguration("myInteger2=66 myInteger3=88", Holder.class);
		
		Assert.assertEquals(66, mc7.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(66, mc7.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(88, mc7.get(Holder.MY_INTEGER_3).intValue());
		
		// If everything is defined, everything get its defined value
		
		MapConfiguration mc8 = new MapConfiguration("myInteger1=6 myInteger2=7 myInteger3=8", Holder.class);
		
		Assert.assertEquals(6, mc8.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(7, mc8.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(8, mc8.get(Holder.MY_INTEGER_3).intValue());
	}
	
	@Test
	public void testMultipleEverything() {
		
		class Holder {
			
			public static final ConfigKey<Integer> MY_INTEGER_1 = intKey().def(10);
			public static final ConfigKey<Integer> MY_INTEGER_2 = intKey().def(20).alias(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_3 = intKey().def(30).deprecated(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_4 = intKey().def(40).alias(MY_INTEGER_1);
			public static final ConfigKey<Integer> MY_INTEGER_5 = intKey().def(50).deprecated(MY_INTEGER_1);
		}
		
		MapConfiguration mc1 = new MapConfiguration(Holder.class);
		
		Assert.assertEquals(10, mc1.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(20, mc1.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(30, mc1.get(Holder.MY_INTEGER_3).intValue());
		Assert.assertEquals(40, mc1.get(Holder.MY_INTEGER_4).intValue());
		Assert.assertEquals(50, mc1.get(Holder.MY_INTEGER_5).intValue());
		
		MapConfiguration mc2 = new MapConfiguration("myInteger1=5", Holder.class);
		
		Assert.assertEquals(5, mc2.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(5, mc2.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(5, mc2.get(Holder.MY_INTEGER_3).intValue());
		Assert.assertEquals(5, mc2.get(Holder.MY_INTEGER_4).intValue());
		Assert.assertEquals(5, mc2.get(Holder.MY_INTEGER_5).intValue());
		
		MapConfiguration mc3 = new MapConfiguration("myInteger2=15 myInteger5=25", Holder.class);
		
		Assert.assertEquals(15, mc3.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(15, mc3.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(15, mc3.get(Holder.MY_INTEGER_3).intValue());
		Assert.assertEquals(15, mc3.get(Holder.MY_INTEGER_4).intValue());
		Assert.assertEquals(25, mc3.get(Holder.MY_INTEGER_5).intValue());
		
		MapConfiguration mc4 = new MapConfiguration("myInteger3=35 myInteger4=45", Holder.class);
		
		Assert.assertEquals(45, mc4.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(45, mc4.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(35, mc4.get(Holder.MY_INTEGER_3).intValue());
		Assert.assertEquals(45, mc4.get(Holder.MY_INTEGER_4).intValue());
		Assert.assertEquals(45, mc4.get(Holder.MY_INTEGER_5).intValue());
		
		MapConfiguration mc5 = new MapConfiguration("myInteger4=33 myInteger5=44", Holder.class);
		
		Assert.assertEquals(33, mc5.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(33, mc5.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(33, mc5.get(Holder.MY_INTEGER_3).intValue());
		Assert.assertEquals(33, mc5.get(Holder.MY_INTEGER_4).intValue());
		Assert.assertEquals(44, mc5.get(Holder.MY_INTEGER_5).intValue());
		
		MapConfiguration mc6 = new MapConfiguration("myInteger2=11 myInteger3=22", Holder.class);
		
		Assert.assertEquals(11, mc6.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(11, mc6.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(22, mc6.get(Holder.MY_INTEGER_3).intValue());
		Assert.assertEquals(11, mc6.get(Holder.MY_INTEGER_4).intValue());
		Assert.assertEquals(11, mc6.get(Holder.MY_INTEGER_5).intValue());

		MapConfiguration mc7 = new MapConfiguration("myInteger5=99", Holder.class);
		
		Assert.assertEquals(99, mc7.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(99, mc7.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(99, mc7.get(Holder.MY_INTEGER_3).intValue());
		Assert.assertEquals(99, mc7.get(Holder.MY_INTEGER_4).intValue());
		Assert.assertEquals(99, mc7.get(Holder.MY_INTEGER_5).intValue());
		
		MapConfiguration mc8 = new MapConfiguration("myInteger1=66 myInteger5=77", Holder.class);
		
		Assert.assertEquals(66, mc8.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(66, mc8.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(66, mc8.get(Holder.MY_INTEGER_3).intValue());
		Assert.assertEquals(66, mc8.get(Holder.MY_INTEGER_4).intValue());
		Assert.assertEquals(77, mc8.get(Holder.MY_INTEGER_5).intValue());
		
		MapConfiguration mc9 = new MapConfiguration("myInteger2=11", Holder.class);
		
		Assert.assertEquals(11, mc9.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(11, mc9.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(11, mc9.get(Holder.MY_INTEGER_3).intValue());
		Assert.assertEquals(11, mc9.get(Holder.MY_INTEGER_4).intValue());
		Assert.assertEquals(11, mc9.get(Holder.MY_INTEGER_5).intValue());
		
		MapConfiguration mc10 = new MapConfiguration("myInteger3=11", Holder.class);
		
		Assert.assertEquals(11, mc10.get(Holder.MY_INTEGER_1).intValue());
		Assert.assertEquals(11, mc10.get(Holder.MY_INTEGER_2).intValue());
		Assert.assertEquals(11, mc10.get(Holder.MY_INTEGER_3).intValue());
		Assert.assertEquals(11, mc10.get(Holder.MY_INTEGER_4).intValue());
		Assert.assertEquals(11, mc10.get(Holder.MY_INTEGER_5).intValue());
	}
}