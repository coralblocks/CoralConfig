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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    }

    public synchronized static ConfigContainer of(Class<?> holder) {
    	ConfigContainer configContainer = ALL.get(holder);
    	if (configContainer == null) {
    		configContainer = new ConfigContainer(holder);
    		ALL.put(holder, configContainer);
    	}
    	return configContainer;
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
    
    public Class<?> getHolder() {
    	return holder;
    }
    
    @Override
    public String toString() {
        return toString;
    }
}
