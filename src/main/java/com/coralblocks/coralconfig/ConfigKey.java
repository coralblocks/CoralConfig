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

public final class ConfigKey<T> {
	
	static enum Kind {
		PRIMARY, ALIAS, DEPRECATED
	}
	
	private final String name;
	private final Class<T> type;
	private final Kind kind;
	private final ConfigKey<T> primary;
	
	private ConfigKey(String name, Class<T> type, Kind kind, ConfigKey<T> primary) {
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.primary = primary;
    }

    public static <T> ConfigKey<T> of(String name, Class<T> type, Kind kind, ConfigKey<T> primary) {
    	return new ConfigKey<T>(name, type, kind, primary);
    }

    public String getName() {
    	return name;
    }
	    
    public Class<T> getType() {
    	return type;
    }
    
    public Kind getKind() {
    	return kind;
    }
    
    public ConfigKey<T> getPrimary() {
    	return primary;
    }
    
	@Override
	public String toString() {
		return name;
	}
}