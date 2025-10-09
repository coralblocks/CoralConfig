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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MapConfiguration implements Configuration {
	
	private final Config config;
	private final Map<ConfigKey<?>, Object> values = Collections.synchronizedMap(new HashMap<ConfigKey<?>, Object>());
	
	public MapConfiguration(Class<?> holder) {
		this(holder, null);
	}
	
	public MapConfiguration(Class<?> holder, String params) {
		this.config = Config.of(holder);
		if (params != null) {
			String[] keyValues = params.split("\\s+");
			for(String keyValue : keyValues) {
				String[] temp = keyValue.split("=");
				if (temp.length != 2) {
					throw new IllegalArgumentException("The params argument is invalid: " + params + " (" + keyValue + ")");
				}
				String key = temp[0];
				String value = temp[1];
				
				ConfigKey<?> configKey = config.get(key);
				if (configKey == null) {
					throw new IllegalStateException("A key in params does not contain a ConfigKey: " + key);
				}
				Object parsedValue = configKey.parseValue(value);
				values.put(configKey, parsedValue);
			}
		}
	}
	
	public MapConfiguration(Configuration config) {
		this.config = Config.of(config.getHolder());
		Set<ConfigKey<?>> set = config.keys();
		Iterator<ConfigKey<?>> iter = set.iterator();
		while(iter.hasNext()) {
			ConfigKey<?> configKey = iter.next();
			Object value = config.get(configKey);
			values.put(configKey, value);
		}
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
	
	@Override
	public Class<?> getHolder() {
		return config.getHolder();
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
	public int size() {
		return values.size();
	}

	@Override
	public Set<ConfigKey<?>> keys() {
		// let's be thread-safe here and return a new Set each time...
		synchronized(values) {
			return new HashSet<ConfigKey<?>>(values.keySet());
		}
	}
}