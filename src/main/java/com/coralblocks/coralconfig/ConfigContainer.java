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

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.coralblocks.coralconfig.ConfigKey.Kind;

final class ConfigContainer {

    private static final Object SCAN_LOCK = new Object();

    private static final ClassValue<ConfigContainer> CONTAINERS = new ClassValue<ConfigContainer>() {

        @Override
        protected ConfigContainer computeValue(Class<?> holder) {
            return new ConfigContainer(holder);
        }
    };

    private final Class<?> holder;
    private final Set<ConfigKey<?>> configKeys;
    private final Map<String, ConfigKey<?>> configKeysByParamName;
    private final String toString;

    private ConfigContainer(Class<?> holder) {
    	
        this.holder = holder;
        
        Map<ConfigKey<?>, String> collected = new LinkedHashMap<ConfigKey<?>, String>();
        
        for(Field f : holder.getDeclaredFields()) {
        	
            int m = f.getModifiers();
            
            if (!Modifier.isStatic(m)) continue;
            
            if (!ConfigKey.class.isAssignableFrom(f.getType())) continue;

            if (!Modifier.isFinal(m)) {
                throw new IllegalStateException("Config key field must be final: " + holder.getName() + "." + f.getName());
            }
            
            try {
            	
                if (!f.canAccess(null)) f.setAccessible(true);
                
                ConfigKey<?> configKey = (ConfigKey<?>) f.get(null);

                if (configKey == null) {
                    throw new IllegalStateException("Config key field is null: " + holder.getName() + "." + f.getName());
                }
                
                String previousField = collected.putIfAbsent(configKey, f.getName());
                if (previousField != null) {
                    throw new IllegalStateException("Config key is declared by multiple fields in holder " + holder.getName() +
                                                    ": " + previousField + " and " + f.getName());
                }
                
            } catch (IllegalAccessException | InaccessibleObjectException e) {
            	
                throw new IllegalStateException("Cannot access config key field: " + holder.getName() + "." + f.getName(), e);
            }
        }
        
        Map<String, ConfigKey<?>> map = new LinkedHashMap<String, ConfigKey<?>>();
        Set<ConfigKey<?>> set = new LinkedHashSet<ConfigKey<?>>();
        
        for(Map.Entry<ConfigKey<?>, String> entry : collected.entrySet()) {
        	
            ConfigKey<?> configKey = entry.getKey();
            String paramName = ConfigKey.toCamelCase(entry.getValue());
            ConfigKey<?> prev = map.putIfAbsent(normalizeParamName(paramName), configKey);
            
            if (prev != null) {
                throw new IllegalStateException("Duplicate config key name: " + paramName + " in holder " + this.holder.getName());
            }
            
            set.add(configKey);
        }
        
        if (set.isEmpty()) throw new IllegalStateException("No config keys found in holder " + this.holder.getName());

        // ClassValue may compute more than once under contention. Serialize only the shared-key mutation phase;
        // relationship lists are replaced rather than appended, so repeated computations remain harmless.
        synchronized(SCAN_LOCK) {
            enforceNotRegisteredByAnotherHolder(holder, set);
            enforcePrimarySameHolder(holder, set);

            for(Map.Entry<ConfigKey<?>, String> entry : collected.entrySet()) {
                entry.getKey().register(entry.getValue(), holder);
            }

            registerRelationships(set);
        }
        
        this.configKeys = Collections.unmodifiableSet(set);
        this.configKeysByParamName = Collections.unmodifiableMap(map);
        
        this.toString = "ConfigContainer[" + holder.getName() + ", size=" + configKeys.size() + "]";
    }

    private static void enforceNotRegisteredByAnotherHolder(Class<?> holder, Set<ConfigKey<?>> configKeys) {
        for(ConfigKey<?> configKey : configKeys) {
            if (configKey.holder != null && configKey.holder != holder) {
                throw new IllegalStateException("Config key already belongs to another holder!" +
                                                " holder=" + holder + " existingHolder=" + configKey.holder);
            }
        }
    }

    private static void registerRelationships(Set<ConfigKey<?>> configKeys) {
        Map<ConfigKey<?>, List<ConfigKey<?>>> aliases = new LinkedHashMap<ConfigKey<?>, List<ConfigKey<?>>>();
        Map<ConfigKey<?>, List<ConfigKey<?>>> deprecated = new LinkedHashMap<ConfigKey<?>, List<ConfigKey<?>>>();

        for(ConfigKey<?> configKey : configKeys) {
            if (configKey.getKind() == Kind.PRIMARY) {
                aliases.put(configKey, new ArrayList<ConfigKey<?>>());
                deprecated.put(configKey, new ArrayList<ConfigKey<?>>());
            }
        }

        for(ConfigKey<?> configKey : configKeys) {
            if (configKey.getKind() == Kind.ALIAS) {
                aliases.get(configKey.getPrimary()).add(configKey);
            } else if (configKey.getKind() == Kind.DEPRECATED) {
                deprecated.get(configKey.getPrimary()).add(configKey);
            }
        }

        for(ConfigKey<?> configKey : aliases.keySet()) {
            configKey.aliases = Collections.unmodifiableList(aliases.get(configKey));
            configKey.deprecated = Collections.unmodifiableList(deprecated.get(configKey));
        }
    }
    
    private static void enforcePrimarySameHolder(Class<?> holder, Set<ConfigKey<?>> configKeys) {
        for(ConfigKey<?> configKey : configKeys) {
            if (configKey.getKind() != Kind.PRIMARY) {
                ConfigKey<?> primary = configKey.getPrimary();
                if (!configKeys.contains(primary)) {
                    throw new IllegalStateException("The primary config key does not contain the same holder!" +
                                                    " holder=" + holder + " primaryHolder=" + primary.holder);
                }
            }
        }
    }

    public static ConfigContainer of(Class<?> holder) {
        return CONTAINERS.get(holder);
    }
    
    public static void enforceNoDuplicates(ConfigContainer ... configContainers) {
    	if (configContainers.length <= 1) {
    		throw new IllegalArgumentException("configContainers must be an array of 2 or more elements! length=" + configContainers.length);
    	}
	    for(int i = 0; i < configContainers.length; i++) {
	        for(int j = i + 1; j < configContainers.length; j++) {
	        	ConfigContainer cc1 = configContainers[i];
	        	ConfigContainer cc2 = configContainers[j];
	        	enforceNoDuplicates(cc1, cc2);
	        }
    	}
    }
    
    static void enforceNoDuplicates(ConfigContainer cc1, ConfigContainer cc2) {
        Iterator<ConfigKey<?>> iter = cc1.configKeys.iterator();
        while(iter.hasNext()) {
            ConfigKey<?> configKey = iter.next();
            ConfigKey<?> duplicate = cc2.get(configKey.getParamName());
            if (duplicate != null) {
                throw new IllegalStateException("Found two keys with the same name! " +
                                                "configKey1=" + configKey + " configKey2=" + duplicate);
            }
        }
    }

    public int size() {
    	return configKeys.size();
    }
    
    public boolean has(ConfigKey<?> configKey) {
    	return configKeys.contains(configKey);
    }

    public ConfigKey<?> get(String paramName) {
        return configKeysByParamName.get(normalizeParamName(paramName));
    }

    private static String normalizeParamName(String paramName) {
        return paramName == null ? null : paramName.toLowerCase(Locale.ROOT);
    }
    
    public Set<ConfigKey<?>> configKeys() {
    	return configKeys;
    }
    
    public Class<?> getHolder() {
    	return holder;
    }
    
    @Override
    public String toString() {
        return toString;
    }
}
