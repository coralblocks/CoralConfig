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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.coralblocks.coralconfig.ConfigKey.Kind;

final class ConfigContainer {

    private static final Map<Class<?>, ConfigContainer> ALL = new HashMap<>();

    private final Class<?> holder;
    private final Set<ConfigKey<?>> configKeys;
    private final Map<String, ConfigKey<?>> configKeysByName;
    private final String toString;

    private ConfigContainer(Class<?> holder) {
    	
        this.holder = holder;
        
        List<ConfigKey<?>> collected = new ArrayList<>();
        
        for(Field f : holder.getDeclaredFields()) {
        	
            int m = f.getModifiers();
            
            if (!Modifier.isStatic(m)) continue;
            
            if (!ConfigKey.class.isAssignableFrom(f.getType())) continue;
            
            try {
            	
                if (!f.canAccess(null)) f.setAccessible(true);
                
                Object val = f.get(null);
                
                ConfigKey<?> configKey = (ConfigKey<?>) val;
                
                configKey.fieldName = f.getName();
                
                configKey.holder = holder;
                
                if (val != null) collected.add(configKey);
                
            } catch (IllegalAccessException e) {
            	
                throw new RuntimeException("Cannot access field: " + f, e);
            }
        }
        
        Map<String, ConfigKey<?>> map = new LinkedHashMap<String, ConfigKey<?>>();
        Set<ConfigKey<?>> set = new HashSet<ConfigKey<?>>();
        
        for(ConfigKey<?> key : collected) {
        	
            String name = key.getName();
            ConfigKey<?> prev = map.putIfAbsent(name, key);
            
            if (prev != null) {
                throw new IllegalStateException("Duplicate ConfigKey name: " + name + " in holder " + this.holder.getName());
            }
            
            set.add(key);
        }
        
        if (set.isEmpty()) throw new IllegalStateException("No ConfigKeys found in holder " + this.holder.getName());
        
        this.configKeys = Collections.synchronizedSet(Collections.unmodifiableSet(set));
        this.configKeysByName = Collections.synchronizedMap(Collections.unmodifiableMap(map));
        
        this.toString = "Config[" + holder.getName() + ", size=" + configKeys.size() + "]";
        
        enforcePrimarySameHolder(configKeys);
        adjustLists(configKeys);
    }
    
    private static void adjustLists(Set<ConfigKey<?>> configKeys) {
    	for(ConfigKey<?> configKey : configKeys) {
    		configKey.aliases = Collections.unmodifiableList(configKey.aliases);
    		configKey.deprecated = Collections.unmodifiableList(configKey.deprecated);
    	}
    }
    
    private static void enforcePrimarySameHolder(Set<ConfigKey<?>> configKeys) {
    	for(ConfigKey<?> configKey : configKeys) {
    		if (configKey.getKind() != Kind.PRIMARY) {
    			ConfigKey<?> primary = configKey.getPrimary();
    			if (primary.holder == null || primary.holder != configKey.holder) {
    				throw new IllegalStateException("The primary key of the config does not contain the same holder!" +
    									" keyHolder=" + configKey.holder + " primaryHolder=" + primary.holder);
    			}
    		}
    	}
    }

    public synchronized static ConfigContainer of(Class<?> holder) {
    	ConfigContainer configContainer = ALL.get(holder);
    	if (configContainer == null) {
    		configContainer = new ConfigContainer(holder);
    		ALL.put(holder, configContainer);
    	}
    	return configContainer;
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
    
    private static void enforceNoDuplicates(ConfigContainer cc1, ConfigContainer cc2) {
    	Iterator<ConfigKey<?>> iter = cc1.configKeys.iterator();
    	while(iter.hasNext()) {
    		ConfigKey<?> configKey = iter.next();
    		ConfigKey<?> duplicate = cc2.getIgnoreCase(configKey.getName());
    		if (duplicate != null) {
    			throw new IllegalStateException("Found two keys with the same name! " +
    									"key1=" + configKey + " key2=" + duplicate);
    		}
    	}
    }

    public int size() {
    	return configKeys.size();
    }
    
    public boolean has(ConfigKey<?> key) {
    	return configKeys.contains(key);
    }

    public ConfigKey<?> get(String name) {
        return configKeysByName.get(name);
    }
    
    private ConfigKey<?> getIgnoreCase(String name) {
    	Iterator<ConfigKey<?>> iter = configKeys.iterator();
    	while(iter.hasNext()) {
    		ConfigKey<?> configKey = iter.next();
    		if (configKey.getName().equalsIgnoreCase(name)) {
    			return configKey;
    		}
    	}
    	return null;
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
