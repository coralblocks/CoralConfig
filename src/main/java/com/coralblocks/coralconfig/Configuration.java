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

/**
 * The common contract for reading configuration values and customizing their fallback defaults.
 * Directly adding or removing configured values is implementation-specific.
 * Invalid inputs or values cause an <code>IllegalArgumentException</code>; a valid request that cannot be resolved from the configuration causes an <code>IllegalStateException</code>.
 * Implementations must be thread-safe: each method call is atomic, and returned collections are immutable snapshots.
 * A sequence of method calls is not one atomic operation.
 */
public interface Configuration {
	
	/**
	 * Adds a <code>DeprecatedListener</code> to receive callbacks. A configuration reports each deprecated key at most once,
	 * when it is first read or configured while at least one listener is registered.
	 * 
	 * @param listener the <code>DeprecatedListener</code> to receive callbacks
	 */
	public void addListener(DeprecatedListener listener);
	
	/**
	 * Removes a <code>DeprecatedListener</code>
	 * 
	 * @param listener the <code>DeprecatedListener</code> to remove
	 */
	public void removeListener(DeprecatedListener listener);
	
	/**
	 * The holder classes which are defining/declaring/specifying the <code>ConfigKey</code>s for this configuration as static final fields.
	 * Note that a configuration can have several holders, not just one.
	 * 
	 * @return a copy of the holder classes that this configuration has
	 */
	public Class<?>[] getHolders();
	
	/**
	 * Returns an unmodifiable snapshot of all <code>ConfigKey</code>s that this configuration has (from all its holder classes).
	 * 
	 * @return an unmodifiable snapshot of all the <code>ConfigKey</code>s for this configuration (from all holders)
	 */
	public List<ConfigKey<?>> allConfigKeys();

	/**
	 * Returns the number of configured values that this configuration has. It can be zero if nothing was added to this configuration.
	 * 
	 * @return the number of configured values
	 */
	public int size();
	
	/**
	 * Gets the value for the given <code>ConfigKey</code>. Resolution proceeds in this order:
	 * <ol>
	 * <li>The configured value of the requested key.</li>
	 * <li>The configured value of its primary, when the requested key is an alias or deprecated key.</li>
	 * <li>Remaining configured aliases and then configured deprecated keys, each in holder declaration order.</li>
	 * <li>Overwritten defaults, using the same group order.</li>
	 * <li>Declared defaults: the requested key's own default; for a required alias or deprecated key, its primary's default; or, for a required primary, the single distinct default supplied by its related keys after lossless conversion to the primary's type.</li>
	 * </ol>
	 * A required key that remains unresolved causes an exception. A required primary also causes an exception when it must borrow a default and its related keys supply multiple distinct values.
	 * 
	 * @param <T> the type of this <code>ConfigKey</code> which can be a Java primitive wrapper (Integer, Short, etc.), a String and an Enum.
	 * @param configKey the <code>ConfigKey</code> to get a value for
	 * @return the value for the given <code>ConfigKey</code>
	 * @throws IllegalArgumentException if the config key is invalid or the resolved value cannot be converted to its type
	 * @throws IllegalStateException if a required config key has no resolvable value or default
	 */
	public <T> T get(ConfigKey<T> configKey);
	
	/**
	 * Returns true if the given <code>ConfigKey</code> has a value associated/configured in this configuration.
	 * 
	 * @param configKey the <code>ConfigKey</code> to check for a value
	 * @return true if the given <code>ConfigKey</code> has a value associated/configured
	 */
	public boolean has(ConfigKey<?> configKey);
	
	/**
	 * Returns an unmodifiable snapshot of all <code>ConfigKey</code>s that have a value associated/configured in this configuration.
	 * 
	 * @return an unmodifiable snapshot containing all the <code>ConfigKey</code>s with values in this configuration
	 */
	public Set<ConfigKey<?>> keys();

	/**
	 * Changes the fallback default returned by this <code>ConfigKey</code> when no configured value exists.
	 * This does not add a configured value, and configured values still take precedence.
	 * 
	 * @param <T> the type of this <code>ConfigKey</code> which can be a Java primitive wrapper (Integer, Short, etc.), a String and an Enum.
	 * @param configKey the <code>ConfigKey</code> to overwrite its default value
	 * @param defaultValue the new default value
	 * @return true if a previous overwritten default value was substituted by the new one
	 * @throws IllegalArgumentException if the config key or default value is invalid
	 * @throws IllegalStateException if the config key has no resolvable default to overwrite
	 */
	public <T> boolean overwriteDefault(ConfigKey<T> configKey, T defaultValue);
	
	/**
	 * Returns the current overwritten default value associated with this <code>ConfigKey</code> or its primary, aliases, or deprecated keys.
	 * 
	 * @param <T> the type of this <code>ConfigKey</code> which can be a Java primitive wrapper (Integer, Short, etc.), a String and an Enum.
	 * @param configKey the <code>ConfigKey</code> for which the overwritten default value will be returned
	 * @return the overwritten default value, or null if it is not defined or was overwritten with null
	 */
	public <T> T getOverwrittenDefault(ConfigKey<T> configKey);
	
	/**
	 * Removes the current overwritten default value associated with this <code>ConfigKey</code>.
	 * 
	 * @param <T> the type of this <code>ConfigKey</code> which can be a Java primitive wrapper (Integer, Short, etc.), a String and an Enum.
	 * @param configKey the <code>ConfigKey</code> for which to remove the current overwritten default value
	 * @return true if an overwritten default value was removed, or false if there was none to remove
	 */
	public <T> boolean removeOverwrittenDefault(ConfigKey<T> configKey);
	
	/**
	 * Removes the overwritten defaults associated with all <code>ConfigKey</code>s that this configuration has.
	 */
	public void removeAllOverwrittenDefaults();
	
	/**
	 * Check if the given <code>ConfigKey</code>, its primary, aliases, or deprecated keys have an associated overwritten default.
	 * 
	 * @param <T> the type of this <code>ConfigKey</code> which can be a Java primitive wrapper (Integer, Short, etc.), a String and an Enum.
	 * @param configKey the <code>ConfigKey</code> for which to check if it has an overwritten default
	 * @return true if it has an overwritten default
	 */
	public <T> boolean hasOverwrittenDefault(ConfigKey<T> configKey);
	
	/**
	 * Returns an unmodifiable snapshot of all <code>ConfigKey</code>s that have overwritten default values.
	 * 
	 * @return an unmodifiable snapshot of all the <code>ConfigKey</code>s with overwritten default values
	 */
	public Set<ConfigKey<?>> keysWithOverwrittenDefault();
}
