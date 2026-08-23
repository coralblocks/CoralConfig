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

import java.math.BigDecimal;
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
 * In addition to the common contract, this implementation supports directly adding and removing configured values through <code>add</code> and <code>remove</code>.
 * This class is thread-safe. Each method call is atomic, but a sequence of calls is not one atomic operation.
 */
public class MapConfiguration implements Configuration {
	
	private final Object lock = new Object();
	private final ConfigContainer[] configContainers;
	private final Class<?>[] holders;
	private final Map<ConfigKey<?>, Object> values = new HashMap<ConfigKey<?>, Object>();
	private final Map<ConfigKey<?>, Object> overwrittenDefaults = new HashMap<ConfigKey<?>, Object>();
	private final List<DeprecatedListener> listeners = new ArrayList<DeprecatedListener>();
	private final Set<ConfigKey<?>> reportedDeprecatedKeys = new HashSet<ConfigKey<?>>();
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
	 * You can pass a list of parameters to configure, for example:
	 * <pre>{@code "myInteger1=2 myString=blah myEnum=BALL myFloat3=3.12"}</pre>
	 * 
	 * @param params some initial values for some of the <code>ConfigKey</code>s of this configuration
	 * @param holders the holder classes from where to get the <code>ConfigKey</code>s
	 */
	public MapConfiguration(String params, Class<?> ... holders) {
		
		if (holders == null || holders.length == 0) throw new IllegalArgumentException("Must pass a holder!");
		
		this.holders = holders.clone();
		enforceNoDuplicateHolders(this.holders);
		
		this.configContainers = new ConfigContainer[this.holders.length];
		for(int i = 0; i < this.holders.length; i++) {
			this.configContainers[i] = ConfigContainer.of(this.holders[i]);
		}
		
		if (configContainers.length > 1) ConfigContainer.enforceNoDuplicates(configContainers); // important!
		
		if (params != null) {
			Map<ConfigKey<?>, String> parsedParams = new HashMap<ConfigKey<?>, String>();
			String[] keyValues = params.trim().split("\\s+");
			for(String keyValue : keyValues) {
				if (keyValue.isEmpty()) continue;
				String[] temp = keyValue.split("=", 2);
				if (temp.length != 2) {
					throw new IllegalArgumentException("The params argument is invalid: " + params + " (" + keyValue + ")");
				}
				String key = temp[0];
				String value = temp[1];
				
				ConfigKey<?> configKey = getByName(key);
				if (configKey == null) {
					throw new IllegalArgumentException("A config key in params does not belong to this configuration: " + key);
				}

				String previous = parsedParams.putIfAbsent(configKey, keyValue);
				if (previous != null) {
					throw new IllegalArgumentException("Duplicate config key in params: " + configKey.getParamName() +
																   " (first: " + previous + ", duplicate: " + keyValue + ")");
				}

				Object parsedValue = configKey.parseValue(value);
				addParsed(configKey, parsedValue);
			}
		}
		
		this.allConfigKeys = gatherAllConfigKeys();
	}
	
	/**
	 * Creates a new <code>MapConfiguration</code> by copying everything from the given configuration.
	 * Listeners are not copied. Copying another <code>MapConfiguration</code> does not notify its listeners.
	 * When the source is another <code>MapConfiguration</code>, its values and overwritten defaults are captured together at one point in time.
	 * 
	 * @param config the configuration to copy everything from for this new <code>MapConfiguration</code>
	 */
	public MapConfiguration(Configuration config) {
		
		this.holders = config.getHolders().clone();
		enforceNoDuplicateHolders(this.holders);
		
		this.configContainers = new ConfigContainer[holders.length];
		for(int i = 0; i < holders.length; i++) {
			this.configContainers[i] = ConfigContainer.of(holders[i]);
		}
		
		if (configContainers.length > 1) ConfigContainer.enforceNoDuplicates(configContainers); // important!
		
		if (config instanceof MapConfiguration) {
			MapConfiguration mapConfig = (MapConfiguration) config;
			synchronized(mapConfig.lock) {
				this.values.putAll(mapConfig.values);
				this.overwrittenDefaults.putAll(mapConfig.overwrittenDefaults);
			}
		} else {
			for(ConfigKey<?> configKey : config.keys()) {
				addCaptured(configKey, config);
			}

			for(ConfigKey<?> configKey : config.keysWithOverwrittenDefault()) {
				overwriteDefaultCaptured(configKey, config);
			}
		}
		
		this.allConfigKeys = gatherAllConfigKeys();
	}

	private static void enforceNoDuplicateHolders(Class<?>[] holders) {
		Set<Class<?>> unique = new HashSet<Class<?>>();
		for(Class<?> holder : holders) {
			if (!unique.add(holder)) throw new IllegalArgumentException("Duplicate holder class: " + holder.getName());
		}
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
			throw new IllegalArgumentException("value cannot be null; remove the config key instead. configKey=" + configKey);
		}
	}
	
	private void enforceDefaultValue(ConfigKey<?> configKey, Object value) {

		if (value != null) return; // nothing to do
		
		Class<?> type = configKey.getType();
		
		if (type == String.class || type.isEnum()) { 
			return; // allow null for String and Enum
		}
		
		throw new IllegalArgumentException("defaultValue can be null only for String and Enum keys. configKey=" + configKey);
	}
	
	private boolean checkConfigContainers(ConfigKey<?> configKey) {
		for(ConfigContainer cc : configContainers) {
			if (cc.has(configKey)) return true;
		}
		return false;
	}
	
	private void enforceConfigKey(ConfigKey<?> configKey) {
		
		if (configKey == null) {
			throw new IllegalArgumentException("configKey cannot be null.");
		}
		
		if (!checkConfigContainers(configKey)) {
			throw new IllegalArgumentException("Config key does not belong to this configuration: " + configKey);
		}
	}

	private static String describeConfigKey(ConfigKey<?> configKey) {
		return "'" + configKey.getParamName() + "' (" + configKey.getFieldName() + " in " + configKey.getHolder().getSimpleName() + ")";
	}
	
	@SuppressWarnings("unchecked")
	private static <T> T coerceNumber(Object value, Class<T> targetType) {

		if (value == null) return null;

		if (targetType.isInstance(value)) return (T) value;

		if (!(value instanceof Number)) {
			throw new IllegalArgumentException("Cannot convert a type that is not a number!" +
											   " valueType=" + value.getClass().getSimpleName() +
											   " targetType=" + targetType.getSimpleName());
		}

		Number number = (Number) value;

		try {
			BigDecimal decimal = toBigDecimal(number);

			if (targetType == Integer.class) return (T) Integer.valueOf(decimal.intValueExact());
			if (targetType == Long.class) return (T) Long.valueOf(decimal.longValueExact());
			if (targetType == Short.class) return (T) Short.valueOf(decimal.shortValueExact());
			if (targetType == Byte.class) return (T) Byte.valueOf(decimal.byteValueExact());
			if (targetType == Float.class) {
				Float converted = Float.valueOf(number.floatValue());
				enforceExactConversion(number, targetType, decimal, new BigDecimal(converted.floatValue()));
				return (T) converted;
			}
			if (targetType == Double.class) {
				Double converted = Double.valueOf(number.doubleValue());
				enforceExactConversion(number, targetType, decimal, new BigDecimal(converted.doubleValue()));
				return (T) converted;
			}
		} catch(ArithmeticException | NumberFormatException e) {
			throw cannotConvertWithoutLoss(number, targetType, e);
		}

		throw new IllegalArgumentException("Unsupported numeric type: " + targetType);
	}

	private static BigDecimal toBigDecimal(Number value) {
		if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
			return BigDecimal.valueOf(value.longValue());
		}
		return new BigDecimal(value.doubleValue());
	}

	private static void enforceExactConversion(Number value, Class<?> targetType, BigDecimal original, BigDecimal converted) {
		if (original.compareTo(converted) != 0) throw cannotConvertWithoutLoss(value, targetType, null);
	}

	private static IllegalArgumentException cannotConvertWithoutLoss(Number value, Class<?> targetType, Exception cause) {
		String message = "Cannot convert numeric value without loss!" +
						 " value=" + value + " valueType=" + value.getClass().getSimpleName() +
						 " targetType=" + targetType.getSimpleName();
		return cause == null ? new IllegalArgumentException(message) : new IllegalArgumentException(message, cause);
	}
	
	@Override
	public List<ConfigKey<?>> allConfigKeys() {
		return allConfigKeys;
	}
	
	@Override
	public void addListener(DeprecatedListener listener) {
		synchronized(lock) {
			if (!listeners.contains(listener)) listeners.add(listener);
		}
	}
	
	@Override
	public void removeListener(DeprecatedListener listener) {
		synchronized(lock) {
			listeners.remove(listener);
		}
	}

	@Override
	public <T> boolean overwriteDefault(ConfigKey<T> configKey, T defaultValue) {
		synchronized(lock) {
			enforceConfigKey(configKey);
			enforceDefaultValue(configKey, defaultValue);
			checkDeprecated(configKey);

			if (!hasResolvableDefault(configKey, collectGroupDefaults(configKey))) {
				throw new IllegalStateException("Config key " + describeConfigKey(configKey) + " has no resolvable default to overwrite.");
			}

			boolean hadAlready = overwrittenDefaults.containsKey(configKey);
			overwrittenDefaults.put(configKey, defaultValue);
			return hadAlready;
		}
	}
	
	@Override
	public Set<ConfigKey<?>> keysWithOverwrittenDefault() {
		synchronized(lock) {
			return Collections.unmodifiableSet(new HashSet<ConfigKey<?>>(overwrittenDefaults.keySet()));
		}
	}
	
	private <T> void checkDeprecated(ConfigKey<T> configKey) {
		
		if (configKey.getKind() == Kind.DEPRECATED && !listeners.isEmpty() && reportedDeprecatedKeys.add(configKey)) {
			for(DeprecatedListener listener : new ArrayList<DeprecatedListener>(listeners)) {
				listener.deprecatedConfig(configKey, configKey.getPrimary());
			}
		}
	}

	@Override
	public <T> T getOverwrittenDefault(ConfigKey<T> configKey) {
		synchronized(lock) {
			enforceConfigKey(configKey);
			Object val = getImpl(configKey, overwrittenDefaults);
			return coerceNumber(val, configKey.getType());
		}
	}
	
	@Override
	public <T> boolean hasOverwrittenDefault(ConfigKey<T> configKey) {
		synchronized(lock) {
			enforceConfigKey(configKey);
			return hasImpl(configKey, overwrittenDefaults);
		}
	}
	
	@Override
	public Class<?>[] getHolders() {
		return holders.clone();
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
		synchronized(lock) {
			enforceConfigKey(configKey);
			enforceValue(configKey, value);
			checkDeprecated(configKey);

			Object prev = values.put(configKey, value);
			return prev != null ? configKey.getType().cast(prev) : null;
		}
	}
	
	/**
	 * Removes the value from the given <code>ConfigKey</code> for this configuration.
	 * 
	 * @param <T> the type of this <code>ConfigKey</code> which can be a Java primitive wrapper (Integer, Short, etc.), a String and an Enum.
	 * @param configKey the <code>ConfigKey</code> for which the value will be removed
	 * @return a previous value that was added for the given <code>ConfigKey</code> or null if there was none
	 */
	public <T> T remove(ConfigKey<T> configKey) {
		synchronized(lock) {
			enforceConfigKey(configKey);
			Object prev = values.remove(configKey);
			return prev != null ? configKey.getType().cast(prev) : null;
		}
	}
	
	@Override
	public <T> boolean removeOverwrittenDefault(ConfigKey<T> configKey) {
		synchronized(lock) {
			enforceConfigKey(configKey);

			if (overwrittenDefaults.containsKey(configKey)) { // it can have NULLs...
				overwrittenDefaults.remove(configKey);
				return true;
			}

			return false;
		}
	}
	
	@Override
	public void removeAllOverwrittenDefaults() {
		synchronized(lock) {
			overwrittenDefaults.clear();
		}
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
	
	private static Set<Object> collectGroupDefaults(ConfigKey<?> configKey) {
		if (!configKey.isRequired() || configKey.getKind() != Kind.PRIMARY) return Collections.emptySet();

		Set<Object> defaults = new HashSet<Object>();

		for(ConfigKey<?> relatedKey : configKey.getAliases()) {
			collectDefault(defaults, relatedKey, configKey.getType());
		}

		for(ConfigKey<?> relatedKey : configKey.getDeprecated()) {
			collectDefault(defaults, relatedKey, configKey.getType());
		}

		return defaults;
	}

	private static void collectDefault(Set<Object> defaults, ConfigKey<?> relatedKey, Class<?> targetType) {
		if (!relatedKey.isRequired()) defaults.add(coerceNumber(relatedKey.getDefaultValue(), targetType));
	}

	private static boolean hasResolvableDefault(ConfigKey<?> configKey, Set<Object> groupDefaults) {
		if (!configKey.isRequired()) return true;
		if (configKey.getKind() != Kind.PRIMARY) return !configKey.getPrimary().isRequired();
		return groupDefaults.size() == 1;
	}
	
	@Override
	public <T> T get(ConfigKey<T> configKey) {
		synchronized(lock) {
			enforceConfigKey(configKey);
			checkDeprecated(configKey);

			Object val = getImpl(configKey, values);
			if (val != null) return coerceNumber(val, configKey.getType());

			Set<Object> groupDefaults = collectGroupDefaults(configKey);
			if (hasResolvableDefault(configKey, groupDefaults)) {
				if (hasImpl(configKey, overwrittenDefaults)) {
					val = getImpl(configKey, overwrittenDefaults);
					return coerceNumber(val, configKey.getType());
				}
			}

			if (configKey.isRequired()) {
				// well, see if its primary has a default..
				if (configKey.getKind() != Kind.PRIMARY) {
					ConfigKey<?> primaryKey = configKey.getPrimary();
					if (!primaryKey.isRequired()) {
						return coerceNumber(primaryKey.getDefaultValue(), configKey.getType());
					}
				} else { // PRIMARY KEY
					if (groupDefaults.size() == 1) {
						val = groupDefaults.iterator().next();
						return coerceNumber(val, configKey.getType());
					} else if (groupDefaults.size() > 1) {
						throw new IllegalStateException("Required config key " + describeConfigKey(configKey) +
								" cannot be resolved because its related keys define " + groupDefaults.size() + " distinct defaults.");
					}
				}

				throw new IllegalStateException("Required config key " + describeConfigKey(configKey) +
										" has no configured value and no default.");
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
	}
	
	@Override
	public boolean has(ConfigKey<?> configKey) {
		synchronized(lock) {
			enforceConfigKey(configKey);
			return hasImpl(configKey, values);
		}
	}
	
	@Override
	public int size() {
		synchronized(lock) {
			return values.size();
		}
	}

	@Override
	public Set<ConfigKey<?>> keys() {
		synchronized(lock) {
			return Collections.unmodifiableSet(new HashSet<ConfigKey<?>>(values.keySet()));
		}
	}
}
