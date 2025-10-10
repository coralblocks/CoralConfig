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
import java.util.List;

public final class ConfigKey<T> {
	
	static enum Kind {
		PRIMARY, ALIAS, DEPRECATED
	}
	
	private final String name;
	private final Class<T> type;
	private final Kind kind;
	private final ConfigKey<?> primary;
	List<ConfigKey<?>> aliases = new ArrayList<ConfigKey<?>>();
	List<ConfigKey<?>> deprecated = new ArrayList<ConfigKey<?>>();
	private String fieldName;
	Class<?> holder;
	
	private ConfigKey(String name, Class<T> type, Kind kind, ConfigKey<?> primary) {
		enforceType(type);
		enforceRelationship(name, type, kind, primary);
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.primary = primary;

        if (primary != null && (primary.aliases.getClass().getName().startsWith("java.util.Collections$Unmodifiable") ||
        		primary.deprecated.getClass().getName().startsWith("java.util.Collections$Unmodifiable"))) return;
        
        if (kind == Kind.ALIAS) {
        	primary.aliases.add(this);
        } else if (kind == Kind.DEPRECATED) {
        	primary.deprecated.add(this);
        }
        if (kind != Kind.PRIMARY) enforceCompatibleType(primary, this, kind);
    }
	
	private static void enforceCompatibleType(ConfigKey<?> primary, ConfigKey<?> other, Kind kind) {
		if (primary.getType() == other.getType()) {
			// good
		} else if (kind == Kind.DEPRECATED && Number.class.isAssignableFrom(primary.getType()) && Number.class.isAssignableFrom(other.getType())) {
			// good
		} else {
			throw new IllegalStateException("The types are incompatible for " + (kind == Kind.ALIAS ? "an alias" : "deprecation") + "!" +
									" primaryType=" + primary.getType().getSimpleName() + " otherType=" + other.getType().getSimpleName() +
									" primary=" + primary + " other=" + other);
		}
	}
	
	private void enforceRelationship(String name, Class<T> type, Kind kind, ConfigKey<?> primary) {
		if (kind == Kind.PRIMARY) {
			if (primary != null) {
				throw new IllegalStateException("When defining a primary config key, it must not have have a parent primary!" +
									" name=" + name + " type=" + type.getSimpleName() + " primary=" + primary);
			}
		} else if (kind == Kind.ALIAS) {
			if (primary == null) {
				throw new IllegalStateException("When defining an alias config key, it must specify its parent primary!" +
						" name=" + name + " type=" + type.getSimpleName());
			} else if (primary.getKind() != Kind.PRIMARY) {
				throw new IllegalStateException("The parent config key of an alias config key must not be an alias or a deprecated type!" +
						" name=" + name + " type=" + type.getSimpleName() + " primary=" + primary + " primaryKind=" + primary.getKind());
			}
		} else if (kind == Kind.DEPRECATED) {
			if (primary == null) {
				throw new IllegalStateException("When defining a deprecated config key, it must specify its parent primary!" +
						" name=" + name + " type=" + type.getSimpleName());
			} else if (primary.getKind() != Kind.PRIMARY) {
				throw new IllegalStateException("The parent config key of a deprecated config key must not be an alias or a deprecated type!" +
						" name=" + name + " type=" + type.getSimpleName() + " primary=" + primary + " primaryKind=" + primary.getKind());
			}
		}
	}
	
	private void enforceType(Class<T> c) {
        if (c == String.class
        		|| c == Integer.class || c == Long.class || c == Boolean.class
	            || c == Double.class  || c == Float.class || c == Short.class
	            || c == Byte.class    || c == Character.class
	            || c.isEnum()) {
        	// We are good!
        	return;
        }
        throw new IllegalStateException("Type can only be a Java primitive (Integer, Boolean, etc.), Enum or String!" +
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

    static <K> ConfigKey<K> of(String name, Class<K> type, Kind kind, ConfigKey<?> primary) {
    	return new ConfigKey<K>(name, type, kind, primary);
    }
    
    public static ConfigKey<Integer> intKey(String name) {
    	return of(name, Integer.class, Kind.PRIMARY, null);
    }
    
    public static ConfigKey<Integer> intKeyDeprecated(String name, ConfigKey<?> primary) {
    	return of(name, Integer.class, Kind.DEPRECATED, primary);
    }
    
    public static ConfigKey<Integer> intKeyAlias(String name, ConfigKey<?> primary) {
    	return of(name, Integer.class, Kind.ALIAS, primary);
    }
    
    public static ConfigKey<Long> longKey(String name) {
    	return of(name, Long.class, Kind.PRIMARY, null);
    }
    
    public static ConfigKey<Long> longKeyDeprecated(String name, ConfigKey<?> primary) {
    	return of(name, Long.class, Kind.DEPRECATED, primary);
    }
    
    public static ConfigKey<Long> longKeyAlias(String name, ConfigKey<?> primary) {
    	return of(name, Long.class, Kind.ALIAS, primary);
    }
    
    public static ConfigKey<Boolean> boolKey(String name) {
    	return of(name, Boolean.class, Kind.PRIMARY, null);
    }
    
    public static ConfigKey<Boolean> boolKeyDeprecated(String name, ConfigKey<?> primary) {
    	return of(name, Boolean.class, Kind.DEPRECATED, primary);
    }
    
    public static ConfigKey<Boolean> boolKeyAlias(String name, ConfigKey<?> primary) {
    	return of(name, Boolean.class, Kind.ALIAS, primary);
    }
    
    public static ConfigKey<Double> doubleKey(String name) {
    	return of(name, Double.class, Kind.PRIMARY, null);
    }
    
    public static ConfigKey<Double> doubleKeyDeprecated(String name, ConfigKey<?> primary) {
    	return of(name, Double.class, Kind.DEPRECATED, primary);
    }
    
    public static ConfigKey<Double> doubleKeyAlias(String name, ConfigKey<?> primary) {
    	return of(name, Double.class, Kind.ALIAS, primary);
    }
    
    public static ConfigKey<Float> floatKey(String name) {
    	return of(name, Float.class, Kind.PRIMARY, null);
    }
    
    public static ConfigKey<Float> floatKeyDeprecated(String name, ConfigKey<?> primary) {
    	return of(name, Float.class, Kind.DEPRECATED, primary);
    }
    
    public static ConfigKey<Float> floatKeyAlias(String name, ConfigKey<?> primary) {
    	return of(name, Float.class, Kind.ALIAS, primary);
    }
    
    public static ConfigKey<Short> shortKey(String name) {
    	return of(name, Short.class, Kind.PRIMARY, null);
    }
    
    public static ConfigKey<Short> shortKeyDeprecated(String name, ConfigKey<?> primary) {
    	return of(name, Short.class, Kind.DEPRECATED, primary);
    }
    
    public static ConfigKey<Short> shortKeyAlias(String name, ConfigKey<?> primary) {
    	return of(name, Short.class, Kind.ALIAS, primary);
    }
    
    public static ConfigKey<Byte> byteKey(String name) {
    	return of(name, Byte.class, Kind.PRIMARY, null);
    }
    
    public static ConfigKey<Byte> byteKeyDeprecated(String name, ConfigKey<?> primary) {
    	return of(name, Byte.class, Kind.DEPRECATED, primary);
    }
    
    public static ConfigKey<Byte> byteKeyAlias(String name, ConfigKey<?> primary) {
    	return of(name, Byte.class, Kind.ALIAS, primary);
    }
    
    public static ConfigKey<Character> charKey(String name) {
    	return of(name, Character.class, Kind.PRIMARY, null);
    }
    
    public static ConfigKey<Character> chatKeyDeprecated(String name, ConfigKey<?> primary) {
    	return of(name, Character.class, Kind.DEPRECATED, primary);
    }
    
    public static ConfigKey<Character> charKeyAlias(String name, ConfigKey<?> primary) {
    	return of(name, Character.class, Kind.ALIAS, primary);
    }
    
    public static <E extends Enum<E>> ConfigKey<E> enumKey(String name, Class<E> enumClass) {
        return ConfigKey.of(name, enumClass, Kind.PRIMARY, null);
    }
    
    public static <E extends Enum<E>> ConfigKey<E> enumKeyDeprecated(String name, Class<E> enumClass ,ConfigKey<?> primary) {
    	return ConfigKey.of(name, enumClass, Kind.DEPRECATED, primary);
    }
    
    public static <E extends Enum<E>> ConfigKey<E> enumKeyAlias(String name, Class<E> enumClass ,ConfigKey<?> primary) {
    	return ConfigKey.of(name, enumClass, Kind.ALIAS, primary);
    }
    
    public static ConfigKey<String> stringKey(String name) {
    	return of(name, String.class, Kind.PRIMARY, null);
    }
    
    public static ConfigKey<String> stringKeyDeprecated(String name, ConfigKey<?> primary) {
    	return of(name, String.class, Kind.DEPRECATED, primary);
    }
    
    public static ConfigKey<String> stringKeyAlias(String name, ConfigKey<?> primary) {
    	return of(name, String.class, Kind.ALIAS, primary);
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
    
    public ConfigKey<?> getPrimary() {
    	return primary;
    }
    
    public Class<?> getHolder() {
    	return holder;
    }
    
    public String getFieldName() {
    	return fieldName;
    }
    
    void setFieldName(String fieldName) {
    	this.fieldName = fieldName;
    }
    
    public List<ConfigKey<?>> getAliases() {
    	return aliases;
    }
    
    public List<ConfigKey<?>> getDeprecated() {
    	return deprecated;
    }
    
	@Override
	public String toString() {
		
		String suffix = "";
		if (kind == Kind.ALIAS) {
			suffix = "_aliasOf_[" + primary + "]";
		} else if (kind == Kind.DEPRECATED) {
			suffix = "_deprecatedInFavorOf_[" + primary + "]";
		}
		
		if (fieldName == null && holder == null) {
			return "(\"" + name + "\")" + suffix;
		} else if (fieldName != null && holder != null) {
			return holder.getSimpleName() + "." + fieldName + "(\"" + name + "\")" + suffix;
		} else if (fieldName != null) {
			return fieldName + "(\"" + name + "\")" + suffix;
		} else { // => holder != null && fieldName == null
			return holder.getSimpleName() + "(\"" + name + "\")" + suffix;
		}
	}
}