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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MapConfiguration implements Configuration {
	
	private final Config config;
	private final Map<ConfigKey<?>, Object> values = new HashMap<ConfigKey<?>, Object>();
	
	public MapConfiguration(Config config) {
		this.config = config;
	}
	
	private void enforceConfigKey(ConfigKey<?> key) {
		if (!config.has(key)) {
			throw new IllegalStateException("ConfigKey does not belong to holder class!" +
											" holder=" + config.getHolder().getName() + 
											" key=" + key + 
											" fieldName=" + key.fieldName);
		}
	}

	@Override
	public <T> void overwriteDefault(ConfigKey<T> key, T defaultValue) {
		// TODO:
	}
	
	public <T> T add(ConfigKey<T> key, T value) {
		enforceConfigKey(key);
		Object prev = values.put(key, value);
		return prev != null ? key.getType().cast(prev) : null;
	}
	
	public <T> T remove(ConfigKey<T> key) {
		enforceConfigKey(key);
		Object prev = values.remove(key);
		return prev != null ? key.getType().cast(prev) : null;
	}
	
	@Override
	public <T> T get(ConfigKey<T> key) {
		enforceConfigKey(key);
		Object val = values.get(key);
		if (val == null) {
			throw new RuntimeException("Expected configuration not found!" +
					" holder=" + config.getHolder().getName() + 
					" key=" + key + 
					" fieldName=" + key.fieldName);
		}
		return key.getType().cast(val);
	}

	@Override
	public <T> T get(ConfigKey<T> key, T defaultValue) {
		enforceConfigKey(key);
		Object val = values.get(key);
		if (val == null) {
			return defaultValue;
		}
		return key.getType().cast(val);
	}

	@Override
	public boolean has(ConfigKey<?> key) {
		enforceConfigKey(key);
		return values.containsKey(key);
	}

	@Override
	public Iterator<ConfigKey<?>> keys() {
		return values.keySet().iterator();
	}
}