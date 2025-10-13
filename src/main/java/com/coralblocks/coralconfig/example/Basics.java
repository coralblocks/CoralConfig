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
package com.coralblocks.coralconfig.example;

import static com.coralblocks.coralconfig.ConfigKey.*;

import com.coralblocks.coralconfig.ConfigKey;
import com.coralblocks.coralconfig.Configuration;
import com.coralblocks.coralconfig.MapConfiguration;

public class Basics {
	
	public static final ConfigKey<Integer> MAX_NUMBER_OF_RETRIES = intKey().def(4); // intKey(4); also works
	
	private final int maxNumberOfRetries;
	
	public Basics(Configuration config) {
		this.maxNumberOfRetries = config.get(MAX_NUMBER_OF_RETRIES);
		System.out.println(MAX_NUMBER_OF_RETRIES + " => " + maxNumberOfRetries);
	}
	
	public static void main(String[] args) {
		MapConfiguration config = new MapConfiguration(Basics.class);
		config.add(MAX_NUMBER_OF_RETRIES, 2);
		
		new Basics(config);
	}
}