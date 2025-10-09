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
	String fieldName;
	Class<?> holder;
	
	private ConfigKey(String name, Class<T> type, Kind kind, ConfigKey<T> primary) {
		enforceType(type);
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.primary = primary;
    }
	
	private static void enforceType(Class<?> c) {
        if (c == String.class
        		|| c == Integer.class || c == Long.class || c == Boolean.class
	            || c == Double.class  || c == Float.class || c == Short.class
	            || c == Byte.class    || c == Character.class
	            || c.isEnum()) {
        	// We are good!
        	return;
        }
        throw new RuntimeException("Type can only be a Java primitive (Integer, Boolean, etc.), Enum or String!" +
				" type=" + c.getName());
	}
	
	public T parseValue(String value) {
		return parseValue(type, value);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <K> K parseValue(Class<K> type, String value) {
	    if (value == null) return null;
	    if (type == String.class) {
	        return (K) value;
	    } else if (type == Integer.class || type == int.class) {
	        return (K) Integer.valueOf(value);
	    } else if (type == Long.class || type == long.class) {
	        return (K) Long.valueOf(value);
	    } else if (type == Boolean.class || type == boolean.class) {
	        return (K) Boolean.valueOf(value);
	    } else if (type == Double.class || type == double.class) {
	        return (K) Double.valueOf(value);
	    } else if (type == Float.class || type == float.class) {
	        return (K) Float.valueOf(value);
	    } else if (type == Short.class || type == short.class) {
	        return (K) Short.valueOf(value);
	    } else if (type == Byte.class || type == byte.class) {
	        return (K) Byte.valueOf(value);
	    } else if (type == Character.class || type == char.class) {
	        if (value.length() != 1) throw new IllegalArgumentException("Invalid char value: " + value);
	        return (K) Character.valueOf(value.charAt(0));
	    } else if (type.isEnum()) {
	        return (K) Enum.valueOf((Class<Enum>) type.asSubclass(Enum.class), value.toUpperCase());
	    } else {
	    	throw new IllegalStateException("This type is not valid/expected: " + type);
	    }
	}

    public static <K> ConfigKey<K> of(String name, Class<K> type, Kind kind, ConfigKey<K> primary) {
    	return new ConfigKey<K>(name, type, kind, primary);
    }
    
    public static ConfigKey<Integer> intKey(String name) {
    	return of(name, Integer.class, Kind.PRIMARY, null);
    }
    
    public static ConfigKey<Long> longKey(String name) {
    	return of(name, Long.class, Kind.PRIMARY, null);
    }
    
    public static ConfigKey<Boolean> boolKey(String name) {
    	return of(name, Boolean.class, Kind.PRIMARY, null);
    }
    
    public static ConfigKey<Double> doubleKey(String name) {
    	return of(name, Double.class, Kind.PRIMARY, null);
    }
    
    public static ConfigKey<Float> floatKey(String name) {
    	return of(name, Float.class, Kind.PRIMARY, null);
    }
    
    public static ConfigKey<Short> shortKey(String name) {
    	return of(name, Short.class, Kind.PRIMARY, null);
    }
    
    public static ConfigKey<Byte> byteKey(String name) {
    	return of(name, Byte.class, Kind.PRIMARY, null);
    }
    
    public static ConfigKey<Character> charKey(String name) {
    	return of(name, Character.class, Kind.PRIMARY, null);
    }
    
    public static <E extends Enum<E>> ConfigKey<E> enumKey(String name, Class<E> enumClass) {
        return ConfigKey.of(name, enumClass, Kind.PRIMARY, null);
    }
    
    public static ConfigKey<String> stringKey(String name) {
    	return of(name, String.class, Kind.PRIMARY, null);
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
		if (fieldName == null && holder == null) {
			return name;
		} else if (fieldName != null && holder != null) {
			return holder.getSimpleName() + "." + fieldName + "(\"" + name + "\")";
		} else if (fieldName != null) {
			return fieldName + "(\"" + name + "\")";
		} else { // => holder != null && fieldName == null
			return holder.getSimpleName() + "(\"" + name + "\")";
		}
	}
}