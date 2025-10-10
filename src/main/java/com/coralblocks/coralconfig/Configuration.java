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

import java.util.List;
import java.util.Set;

public interface Configuration {
	
	public void addListener(DeprecatedListener listener);
	
	public void removeListener(DeprecatedListener listener);
	

	public Class<?>[] getHolders();
	
	public List<ConfigKey<?>> allConfigKeys();

	
	public int size();
	
	public <T> T get(ConfigKey<T> configKey);
	
	public boolean has(ConfigKey<?> configKey);
	
	public Set<ConfigKey<?>> keys();

	
	public <T> boolean overwriteDefault(ConfigKey<T> configKey, T defaultValue);
	
	public <T> T getOverwrittenDefault(ConfigKey<T> configKey);
	
	public <T> boolean removeOverwrittenDefault(ConfigKey<T> configKey);
	
	public Set<ConfigKey<?>> keysWithOverwrittenDefault();
}