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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.coralblocks.coralconfig.ConfigKey.Kind;

public class MapConfiguration implements Configuration {
	
	private final ConfigContainer[] configContainers;
	private final Class<?>[] holders;
	private final Map<ConfigKey<?>, Object> values = Collections.synchronizedMap(new HashMap<ConfigKey<?>, Object>());
	private final Map<ConfigKey<?>, Object> overwrittenDefaults = Collections.synchronizedMap(new HashMap<ConfigKey<?>, Object>());
	private final List<DeprecatedListener> listeners = new ArrayList<DeprecatedListener>();
	private final List<ConfigKey<?>> allConfigKeys;
	
	public MapConfiguration(Class<?> ... holders) {
		this(null, holders);
	}
	
	public MapConfiguration(String params, Class<?> ... holders) {
		
		this.holders = holders;
		
		this.configContainers = new ConfigContainer[holders.length];
		for(int i = 0; i < holders.length; i++) {
			this.configContainers[i] = ConfigContainer.of(holders[i]);
		}
		
		if (configContainers.length > 1) ConfigContainer.enforceNoDuplicates(configContainers); // important!
		
		if (params != null) {
			String[] keyValues = params.split("\\s+");
			for(String keyValue : keyValues) {
				String[] temp = keyValue.split("=");
				if (temp.length != 2) {
					throw new IllegalArgumentException("The params argument is invalid: " + params + " (" + keyValue + ")");
				}
				String key = temp[0];
				String value = temp[1];
				
				ConfigKey<?> configKey = getByName(key);
				if (configKey == null) {
					throw new IllegalStateException("A config key in params does not belong to this configuration: " + key);
				}
				Object parsedValue = configKey.parseValue(value);
				addParsed(configKey, parsedValue);
			}
		}
		
		this.allConfigKeys = gatherAllConfigKeys();
	}
	
	public MapConfiguration(Configuration config) {
		
		this.holders = config.getHolders();
		
		this.configContainers = new ConfigContainer[holders.length];
		for(int i = 0; i < holders.length; i++) {
			this.configContainers[i] = ConfigContainer.of(holders[i]);
		}
		
		if (configContainers.length > 1) ConfigContainer.enforceNoDuplicates(configContainers); // important!
		
		for(ConfigKey<?> configKey : config.keys()) {
			addCaptured(configKey, config);
		}
		
		// Copy overwritten defaults too
		
		for(ConfigKey<?> configKey : config.keysWithOverwrittenDefault()) {
			overwriteDefaultCaptured(configKey, config);
		}
		
		this.allConfigKeys = gatherAllConfigKeys();
	}
	
	private List<ConfigKey<?>> gatherAllConfigKeys() {
		List<ConfigKey<?>> list = new ArrayList<ConfigKey<?>>();
		for(ConfigContainer cc : configContainers) {
			for(ConfigKey<?> configKey : cc.configKeys()) {
				list.add(configKey);
			}
		}
		return Collections.unmodifiableList(list);
	}
	
	// // for generics to work, we need a new method to capture the T from the ConfigKey
	private <T> void addParsed(ConfigKey<T> configKey, Object parsed) {
	    add(configKey, configKey.getType().cast(parsed));
	}
	
	// for generics to work, we need a new method to capture the T from the ConfigKey
	private <T> void addCaptured(ConfigKey<T> configKey, Configuration configuration) {
	    T value = configuration.get(configKey);
	    add(configKey, value);
	}
	
	// for generics to work, we need a new method to capture the T from the ConfigKey
	private <T> void overwriteDefaultCaptured(ConfigKey<T> configKey, Configuration configuration) {
	    T value = configuration.getOverwrittenDefault(configKey);
	    overwriteDefault(configKey, value);
	}
	
	private ConfigKey<?> getByName(String name) {
		for(ConfigContainer cc : configContainers) {
			ConfigKey<?> configKey = cc.get(name);
			if (configKey != null) return configKey;
		}
		return null;
	}
	
	private void enforceValue(ConfigKey<?> configKey, Object value) {
		if (value == null) {
			throw new RuntimeException("Null values are not allowed! (You should remove the config key from the configuration instead)" + 
									   " configKey=" + configKey);
		}
	}
	
	private void enforceDefaultValue(ConfigKey<?> configKey, Object value) {

		if (value != null) return; // nothing to do
		
		Class<?> type = configKey.getType();
		
		if (type == String.class || type.isEnum()) { 
			return; // allow null for String and Enum
		}
		
		throw new RuntimeException("Null default values are only allowed for Strings and Enums!" + 
				   " configKey=" + configKey);
	}
	
	private boolean checkConfigContainers(ConfigKey<?> configKey) {
		for(ConfigContainer cc : configContainers) {
			if (cc.has(configKey)) return true;
		}
		return false;
	}
	
	private void enforceConfigKey(ConfigKey<?> configKey) {
		
		if (configKey == null) {
			throw new NullPointerException("The config key can never be null!");
		}
		
		if (!checkConfigContainers(configKey)) {
			throw new IllegalStateException("ConfigKey does not belong to holder class!" +
											" configKey=" + configKey); 
		}
	}
	
	@SuppressWarnings("unchecked")
	private static <T> T coerceNumber(Object value, Class<T> targetType) {
		
	    if (value == null) return null;

	    if (targetType.isInstance(value)) {
	        return (T) value;
	    }

	    if (!(value instanceof Number))
	        throw new IllegalArgumentException("Cannot convert a type that is not a number!" + 
	        			" valueType=" + value.getClass().getSimpleName() +
	        			" targetType=" + targetType.getSimpleName());

	    Number n = (Number) value;

	    if (targetType == Integer.class)
	        return (T) Integer.valueOf(n.intValue());
	    if (targetType == Long.class)
	        return (T) Long.valueOf(n.longValue());
	    if (targetType == Float.class)
	        return (T) Float.valueOf(n.floatValue());
	    if (targetType == Double.class)
	        return (T) Double.valueOf(n.doubleValue());
	    if (targetType == Short.class)
	        return (T) Short.valueOf(n.shortValue());
	    if (targetType == Byte.class)
	        return (T) Byte.valueOf(n.byteValue());

	    throw new IllegalArgumentException("Unsupported numeric type: " + targetType);
	}
	
	@Override
	public List<ConfigKey<?>> allConfigKeys() {
		return allConfigKeys;
	}
	
	@Override
	public void addListener(DeprecatedListener listener) {
		if (!listeners.contains(listener)) listeners.add(listener);
	}
	
	@Override
	public void removeListener(DeprecatedListener listener) {
		listeners.remove(listener);
	}

	@Override
	public <T> boolean overwriteDefault(ConfigKey<T> configKey, T defaultValue) {
		enforceConfigKey(configKey);
		enforceDefaultValue(configKey, defaultValue);
		
		if (configKey.getKind() == Kind.DEPRECATED) {
			for(int i = 0; i < listeners.size(); i++) {
				listeners.get(i).deprecatedConfig(configKey, configKey.getPrimary());
			}
		}
		
		boolean hadAlready = overwrittenDefaults.containsKey(configKey);
			
		overwrittenDefaults.put(configKey, defaultValue);
		
		return hadAlready;
	}
	
	@Override
	public Set<ConfigKey<?>> keysWithOverwrittenDefault() {
		return Collections.unmodifiableSet(overwrittenDefaults.keySet());
	}

	@Override
	public <T> T getOverwrittenDefault(ConfigKey<T> configKey) {
		enforceConfigKey(configKey);
		
		if (configKey.getKind() == Kind.DEPRECATED) {
			for(int i = 0; i < listeners.size(); i++) {
				listeners.get(i).deprecatedConfig(configKey, configKey.getPrimary());
			}
		}
		
		Object val = overwrittenDefaults.get(configKey);
		return val != null ? configKey.getType().cast(val) : null;
	}
	
	@Override
	public Class<?>[] getHolders() {
		return holders;
	}
	
	public <T> T add(ConfigKey<T> configKey, T value) {
		enforceConfigKey(configKey);
		enforceValue(configKey, value);
		
		if (configKey.getKind() == Kind.DEPRECATED) {
			for(int i = 0; i < listeners.size(); i++) {
				listeners.get(i).deprecatedConfig(configKey, configKey.getPrimary());
			}
		}
		
		Object prev = values.put(configKey, value);
		return prev != null ? configKey.getType().cast(prev) : null;
	}
	
	public <T> T remove(ConfigKey<T> configKey) {
		
		enforceConfigKey(configKey);
		
		if (configKey.getKind() == Kind.DEPRECATED) {
			for(int i = 0; i < listeners.size(); i++) {
				listeners.get(i).deprecatedConfig(configKey, configKey.getPrimary());
			}
		}
		
		Object prev = values.remove(configKey);
		return prev != null ? configKey.getType().cast(prev) : null;
	}
	
	@Override
	public <T> boolean removeOverwrittenDefault(ConfigKey<T> configKey) {
		
		enforceConfigKey(configKey);
		
		if (configKey.getKind() == Kind.DEPRECATED) {
			for(int i = 0; i < listeners.size(); i++) {
				listeners.get(i).deprecatedConfig(configKey, configKey.getPrimary());
			}
		}
		
		if (overwrittenDefaults.containsKey(configKey)) { // it can have NULLs...
			overwrittenDefaults.remove(configKey);
			return true;
		}
		
		return false;
	}
	
	private static <T> Object getImpl(ConfigKey<T> ck, Map<ConfigKey<?>, Object> values) {
		
		if (ck.getKind() != Kind.PRIMARY) {
			
			Object val = values.get(ck);
			if (val != null) return val;
			
			val = values.get(ck.getPrimary());
			if (val != null) return val;

			return null;
			
		} else {
			
			Object val = values.get(ck);
			if (val != null) return val;
			
			for(ConfigKey<?> configKey : ck.getAliases()) {
				val = values.get(configKey);
				if (val != null) return val;
			}
			
			for(ConfigKey<?> configKey : ck.getDeprecated()) {
				val = values.get(configKey);
				if (val != null) return val;
			}
			
			return null;
		}
	}
	
	private static <T> boolean hasImpl(ConfigKey<T> ck, Map<ConfigKey<?>, Object> values) {
		
		if (ck.getKind() != Kind.PRIMARY) {
			
			boolean has = values.containsKey(ck);
			if (has) return true;
			
			has = values.containsKey(ck.getPrimary());
			if (has) return true;

			return false;
			
		} else {
			
			boolean has = values.containsKey(ck);
			if (has) return true;
			
			for(ConfigKey<?> configKey : ck.getAliases()) {
				has = values.containsKey(configKey);
				if (has) return true;
			}
			
			for(ConfigKey<?> configKey : ck.getDeprecated()) {
				has = values.containsKey(configKey);
				if (has) return true;
			}
			
			return false;
		}
	}
	
	@Override
	public <T> T get(ConfigKey<T> configKey) {
		
		enforceConfigKey(configKey);
		
		if (configKey.getKind() == Kind.DEPRECATED) {
			for(int i = 0; i < listeners.size(); i++) {
				listeners.get(i).deprecatedConfig(configKey, configKey.getPrimary());
			}
		}
		
		Object val = getImpl(configKey, values);
		if (val != null) return coerceNumber(val, configKey.getType());
		
		throw new RuntimeException("Expected configuration not found!" +
									" configKey=" + configKey);
	}
	
	@Override
	public boolean has(ConfigKey<?> configKey) {
		
		enforceConfigKey(configKey);
		
		if (configKey.getKind() == Kind.DEPRECATED) {
			for(int i = 0; i < listeners.size(); i++) {
				listeners.get(i).deprecatedConfig(configKey, configKey.getPrimary());
			}
		}
		
		return hasImpl(configKey, values);
	}
	
	@Override
	public <T> T get(ConfigKey<T> configKey, T defaultValue) {
		
		enforceConfigKey(configKey);
		
		enforceDefaultValue(configKey, defaultValue);
		
		if (configKey.getKind() == Kind.DEPRECATED) {
			for(int i = 0; i < listeners.size(); i++) {
				listeners.get(i).deprecatedConfig(configKey, configKey.getPrimary());
			}
		}
		
		Object val = getImpl(configKey, values);
		if (val != null) return coerceNumber(val, configKey.getType());
		
		if (hasImpl(configKey, overwrittenDefaults)) {
			val = getImpl(configKey, overwrittenDefaults);
			if (val != null) {
				return coerceNumber(val, configKey.getType());
			} else {
				return null; // Defaults can contain NULL !!!
			}
		}
		
		return defaultValue;
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