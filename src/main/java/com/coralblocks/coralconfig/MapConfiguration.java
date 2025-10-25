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

/**
 * The main implementation of the <code>Configuration</code> interface. It performs a bunch of checks to enforce uniqueness of <code>ConfigKey</code>s and much more.
 */
public class MapConfiguration implements Configuration {
	
	private final ConfigContainer[] configContainers;
	private final Class<?>[] holders;
	private final Map<ConfigKey<?>, Object> values = Collections.synchronizedMap(new HashMap<ConfigKey<?>, Object>());
	private final Map<ConfigKey<?>, Object> overwrittenDefaults = Collections.synchronizedMap(new HashMap<ConfigKey<?>, Object>());
	private final List<DeprecatedListener> listeners = new ArrayList<DeprecatedListener>();
	private final List<ConfigKey<?>> allConfigKeys;
	
	/**
	 * Creates a new <code>MapConfiguration</code> with the <code>ConfigKey</code>s present in the given list of holder classes.
	 * 
	 * @param holders the holder classes from where to get the <code>ConfigKey</code>s
	 */
	public MapConfiguration(Class<?> ... holders) {
		this(null, holders);
	}
	
	/**
	 * Creates a new <code>MapConfiguration</code> with the <code>ConfigKey</code>s present in the given list of holder classes.
	 * You can pass a list of parameters to be configured, for example:
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<code>"myInteger1=2 myString=blah myEnum=BALL myFloat3=3.12"</code>
	 * 
	 * @param params some initial values for some of the <code>ConfigKey</code>s of this configuration
	 * @param holders the holder classes from where to get the <code>ConfigKey</code>s
	 */
	public MapConfiguration(String params, Class<?> ... holders) {
		
		if (holders == null || holders.length == 0) throw new IllegalArgumentException("Must pass a holder!");
		
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
	
	/**
	 * Creates a new <code>MapConfiguration</code> by copying everything from the given configuration.
	 * 
	 * @param config the configuration to copy everything from for this new <code>MapConfiguration</code>
	 */
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
		return Collections.unmodifiableList(allConfigKeys);
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
		
		checkDeprecated(configKey);

		if (!collectAndCheckIfDefaultExists(configKey)) {
			throw new IllegalStateException("The configKey will not (or cannot) return a default value, so it cannot be overwritten! " +
											" configKey=" + configKey + " defaultValue=" + defaultValue);
		}
		
		boolean hadAlready = overwrittenDefaults.containsKey(configKey);
			
		overwrittenDefaults.put(configKey, defaultValue);
		
		return hadAlready;
	}
	
	@Override
	public Set<ConfigKey<?>> keysWithOverwrittenDefault() {
		return Collections.unmodifiableSet(overwrittenDefaults.keySet());
	}
	
	private <T> void checkDeprecated(ConfigKey<T> configKey) {
		
		if (configKey.getKind() == Kind.DEPRECATED) {
			for(int i = 0; i < listeners.size(); i++) {
				listeners.get(i).deprecatedConfig(configKey, configKey.getPrimary());
			}
		}
	}

	@Override
	public <T> T getOverwrittenDefault(ConfigKey<T> configKey) {
		
		enforceConfigKey(configKey);
		
		checkDeprecated(configKey);
		
		Object val = overwrittenDefaults.get(configKey);
		return val != null ? configKey.getType().cast(val) : null;
	}
	
	@Override
	public <T> boolean hasOverwrittenDefault(ConfigKey<T> configKey) {
		
		enforceConfigKey(configKey);
		
		checkDeprecated(configKey);
		
		return overwrittenDefaults.containsKey(configKey);
	}
	
	@Override
	public Class<?>[] getHolders() {
		return holders;
	}
	
	/**
	 * Adds a given value to the given <code>ConfigKey</code> for this configuration.
	 * 
	 * @param <T> the type of this <code>ConfigKey</code> which can be a Java primitive wrapper (Integer, Short, etc.), a String and an Enum.
	 * @param configKey the <code>ConfigKey</code> for which a value will be added to this configuration
	 * @param value the value to be added for the given <code>ConfigKey</code>
	 * @return a previous value that was added for the given <code>ConfigKey</code> or null if there was none
	 */
	public <T> T add(ConfigKey<T> configKey, T value) {
		
		enforceConfigKey(configKey);
		
		enforceValue(configKey, value);
		
		checkDeprecated(configKey);
		
		Object prev = values.put(configKey, value);
		return prev != null ? configKey.getType().cast(prev) : null;
	}
	
	/**
	 * Removes the value from the given <code>ConfigKey</code> for this configuration.
	 * 
	 * @param <T> the type of this <code>ConfigKey</code> which can be a Java primitive wrapper (Integer, Short, etc.), a String and an Enum.
	 * @param configKey the <code>ConfigKey</code> for which the value will be removed
	 * @return a previous value that was added for the given <code>ConfigKey</code> or null if there was none
	 */
	public <T> T remove(ConfigKey<T> configKey) {
		
		enforceConfigKey(configKey);
		
		checkDeprecated(configKey);
		
		Object prev = values.remove(configKey);
		return prev != null ? configKey.getType().cast(prev) : null;
	}
	
	@Override
	public <T> boolean removeOverwrittenDefault(ConfigKey<T> configKey) {
		
		enforceConfigKey(configKey);
		
		checkDeprecated(configKey);
		
		if (overwrittenDefaults.containsKey(configKey)) { // it can have NULLs...
			overwrittenDefaults.remove(configKey);
			return true;
		}
		
		return false;
	}
	
	@Override
	public void removeAllOverwrittenDefaults() {
		overwrittenDefaults.clear();
	}
	
	private static <T> Object getImpl(ConfigKey<T> ck, Map<ConfigKey<?>, Object> values) {
		
		if (ck.getKind() != Kind.PRIMARY) {
			
			Object val = values.get(ck);
			if (val != null) return val;
			
			val = values.get(ck.getPrimary());
			if (val != null) return val;
			
			val = getImpl(ck.getPrimary(), values); // recursive call
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
			
			has = hasImpl(ck.getPrimary(), values); // recursive call
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
	
	private static boolean collectAndCheckIfDefaultExists(ConfigKey<?> configKey) {

		if (!configKey.isRequired()) return true; // it has a default!
		
		// well, see if its primary has a default..
		if (configKey.getKind() != Kind.PRIMARY) {
			ConfigKey<?> primaryKey = configKey.getPrimary();
			if (!primaryKey.isRequired()) {
				return true;
			}
			
		} else { // PRIMARY KEY
			
			Set<Object> collect = new HashSet<Object>();
			
			for(ConfigKey<?> ck : configKey.getAliases()) {
				if (!ck.isRequired()) collect.add(ck.getDefaultValue());
			}
			
			for(ConfigKey<?> ck : configKey.getDeprecated()) {
				if (!ck.isRequired()) collect.add(ck.getDefaultValue());
			}
			
			if (collect.size() == 1) return true;
		}
		
		return false;
	}
	
	@Override
	public <T> T get(ConfigKey<T> configKey) {
		
		enforceConfigKey(configKey);
		
		checkDeprecated(configKey);
		
		Object val = getImpl(configKey, values);
		if (val != null) return coerceNumber(val, configKey.getType());
		
		// check if it will return a default:
		boolean willReturnDefault = collectAndCheckIfDefaultExists(configKey);
		if (willReturnDefault) {
			if (hasImpl(configKey, overwrittenDefaults)) {
				val = getImpl(configKey, overwrittenDefaults);
				return coerceNumber(val, configKey.getType());
			}
		} else {
			// let it roll (there is some redundant logic ahead, but for simplicity let it roll
		}
		
		if (configKey.isRequired()) {
			
			// well, see if its primary has a default..
			if (configKey.getKind() != Kind.PRIMARY) {
				ConfigKey<?> primaryKey = configKey.getPrimary();
				if (!primaryKey.isRequired()) {
					return coerceNumber(primaryKey.getDefaultValue(), configKey.getType());
				}
				
			} else { // PRIMARY KEY
				
				Set<Object> collect = new HashSet<Object>();
				
				for(ConfigKey<?> ck : configKey.getAliases()) {
					if (!ck.isRequired()) collect.add(ck.getDefaultValue());
				}
				
				for(ConfigKey<?> ck : configKey.getDeprecated()) {
					if (!ck.isRequired()) collect.add(ck.getDefaultValue());
				}
				
				if (collect.size() == 1) {
					val = collect.iterator().next();
					return coerceNumber(val, configKey.getType());
				} else if (collect.size() > 1) {
					throw new RuntimeException("More than one default value found!" +
							" configKey=" + configKey + " numberOfDefaults=" + collect.size());
				}
			}
			
			throw new RuntimeException("Expected config key not found!" +
									" configKey=" + configKey);
		}
		
		if (hasImpl(configKey, overwrittenDefaults)) {
			val = getImpl(configKey, overwrittenDefaults);
			if (val != null) {
				return coerceNumber(val, configKey.getType());
			} else {
				return null; // Defaults can contain NULL !!!
			}
		}
		
		return configKey.getDefaultValue();
	}
	
	@Override
	public boolean has(ConfigKey<?> configKey) {
		
		enforceConfigKey(configKey);
		
		checkDeprecated(configKey);
		
		return hasImpl(configKey, values);
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