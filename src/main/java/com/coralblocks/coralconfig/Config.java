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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Config {

    private static final Map<Class<?>, Config> ALL = new HashMap<>();

    private final Class<?> holder;
    private final List<ConfigKey<?>> configKeys;
    private final Map<String, ConfigKey<?>> configKeysByName;
    private final String toString;

    private Config(Class<?> holder) {
    	
        this.holder = holder;
        
        List<ConfigKey<?>> collected = new ArrayList<>();
        
        for(Field f : holder.getDeclaredFields()) {
        	
            int m = f.getModifiers();
            
            if (!Modifier.isStatic(m)) continue;
            
            if (!ConfigKey.class.isAssignableFrom(f.getType())) continue;
            
            try {
            	
                if (!f.canAccess(null)) f.setAccessible(true);
                
                Object val = f.get(null);
                
                if (val != null) collected.add((ConfigKey<?>) val);
                
            } catch (IllegalAccessException e) {
            	
                throw new RuntimeException("Cannot access field: " + f, e);
            }
        }
        
        Map<String, ConfigKey<?>> map = new LinkedHashMap<>();
        
        for(ConfigKey<?> key : collected) {
        	
            String name = key.getName();
            ConfigKey<?> prev = map.putIfAbsent(name, key);
            
            if (prev != null) {
                throw new IllegalStateException("Duplicate ConfigKey name: " + name + " in holder " + this.holder.getName());
            }
        }
        
        this.configKeys = Collections.synchronizedList(Collections.unmodifiableList(collected));
        this.configKeysByName = Collections.synchronizedMap(Collections.unmodifiableMap(map));
        
        this.toString = "Config[" + holder.getName() + ", size=" + configKeys.size() + "]";
    }

    public synchronized static Config of(Class<?> holder) {
    	Config config = ALL.get(holder);
    	if (config == null) {
    		config = new Config(holder);
    		ALL.put(holder, config);
    	}
    	return config;
    }

    public List<ConfigKey<?>> getConfigKeys() {
        return configKeys;
    }
    
    public int size() {
    	return configKeys.size();
    }

    public ConfigKey<?> get(String name) {
        return configKeysByName.get(name);
    }
    
    @Override
    public String toString() {
        return toString;
    }
}
