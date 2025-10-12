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
	
	private String name;
	private final Class<T> type;
	private final Kind kind;
	private final ConfigKey<?> primary;
	List<ConfigKey<?>> aliases = new ArrayList<ConfigKey<?>>();
	List<ConfigKey<?>> deprecated = new ArrayList<ConfigKey<?>>();
	private String fieldName;
	Class<?> holder;
	private final T defaultValue;
	private final boolean isRequired;
	
	private ConfigKey(Class<T> type, Kind kind, boolean isRequired, T defaultValue, ConfigKey<?> primary) {
		enforceType(type);
		enforceRelationship(type, kind, primary);
        this.type = type;
        this.kind = kind;
        this.isRequired = isRequired;
        this.defaultValue = defaultValue;
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
	
	private void enforceRelationship(Class<T> type, Kind kind, ConfigKey<?> primary) {
		if (kind == Kind.PRIMARY) {
			if (primary != null) {
				throw new IllegalStateException("When defining a primary config key, it must not have have a parent primary!" +
									" type=" + type.getSimpleName() + " primary=" + primary);
			}
		} else if (kind == Kind.ALIAS) {
			if (primary == null) {
				throw new IllegalStateException("When defining an alias config key, it must specify its parent primary!" +
						" type=" + type.getSimpleName());
			} else if (primary.getKind() != Kind.PRIMARY) {
				throw new IllegalStateException("The parent config key of an alias config key must not be an alias or a deprecated type!" +
						" type=" + type.getSimpleName() + " primary=" + primary + " primaryKind=" + primary.getKind());
			}
		} else if (kind == Kind.DEPRECATED) {
			if (primary == null) {
				throw new IllegalStateException("When defining a deprecated config key, it must specify its parent primary!" +
						" type=" + type.getSimpleName());
			} else if (primary.getKind() != Kind.PRIMARY) {
				throw new IllegalStateException("The parent config key of a deprecated config key must not be an alias or a deprecated type!" +
						" type=" + type.getSimpleName() + " primary=" + primary + " primaryKind=" + primary.getKind());
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

    static <K> ConfigKey<K> of(Class<K> type, Kind kind, boolean isRequired, K defaultValue, ConfigKey<?> primary) {
    	return new ConfigKey<K>(type, kind, isRequired, defaultValue, primary);
    }
    
    public ConfigKey<T> def(T defaultValue) {
    	if (!isRequired()) {
    		throw new IllegalStateException("Trying to set up a default value twice! " +
    										"previousDefault=" + getDefaultValue() + " newDefault=" + defaultValue);
    	}
    	return of(getType(), getKind(), false, defaultValue, getPrimary());
    }
    
    public ConfigKey<T> deprecated(ConfigKey<?> primary) {
    	if (getKind() == Kind.DEPRECATED) {
    		throw new IllegalStateException("Tried to call deprecated twice on the same config key!");
    	} else if (getKind() == Kind.ALIAS) {
    		throw new IllegalStateException("Tried to call deprecated on a config key that is already an alias!");
    	}
    	return of(getType(), Kind.DEPRECATED, isRequired(), getDefaultValue(), primary);
    }
    
    public ConfigKey<T> alias(ConfigKey<T> primary) {
    	if (getKind() == Kind.ALIAS) {
    		throw new IllegalStateException("Tried to call alias twice on the same config key!");
    	} else if (getKind() == Kind.DEPRECATED) {
    		throw new IllegalStateException("Tried to call alias on a config key that is already deprecated!");
    	}
    	
    	return of(getType(), Kind.ALIAS, isRequired(), getDefaultValue(), primary);
    }
    
    public static ConfigKey<Integer> intKey() {
    	return of(Integer.class, Kind.PRIMARY, true, null, null);
    }
    
    public static ConfigKey<Integer> intKey(Integer defeaultValue) {
    	return of(Integer.class, Kind.PRIMARY, false, defeaultValue, null);
    }
    
    public static ConfigKey<Long> longKey() {
    	return of(Long.class, Kind.PRIMARY, true, null, null);
    }
    
    public static ConfigKey<Long> longKey(Long defaultValue) {
    	return of(Long.class, Kind.PRIMARY, false, defaultValue, null);
    }
    
    public static ConfigKey<Boolean> boolKey() {
    	return of(Boolean.class, Kind.PRIMARY, true, null, null);
    }
    
    public static ConfigKey<Boolean> boolKey(Boolean defaultValue) {
    	return of(Boolean.class, Kind.PRIMARY, false, defaultValue, null);
    }
    
    public static ConfigKey<Double> doubleKey() {
    	return of(Double.class, Kind.PRIMARY, true, null, null);
    }
    
    public static ConfigKey<Double> doubleKey(Double defaultValue) {
    	return of(Double.class, Kind.PRIMARY, false, defaultValue, null);
    }
    
    public static ConfigKey<Float> floatKey() {
    	return of(Float.class, Kind.PRIMARY, true, null, null);
    }
    
    public static ConfigKey<Float> floatKey(Float defaultValue) {
    	return of(Float.class, Kind.PRIMARY, false, defaultValue, null);
    }
    
    public static ConfigKey<Short> shortKey() {
    	return of(Short.class, Kind.PRIMARY, true, null, null);
    }
    
    public static ConfigKey<Short> shortKey(Short defaultValue) {
    	return of(Short.class, Kind.PRIMARY, false, defaultValue, null);
    }
    
    public static ConfigKey<Byte> byteKey() {
    	return of(Byte.class, Kind.PRIMARY, true, null, null);
    }
    
    public static ConfigKey<Byte> byteKey(Byte defaultValue) {
    	return of(Byte.class, Kind.PRIMARY, false, defaultValue, null);
    }
    
    public static ConfigKey<Character> charKey() {
    	return of(Character.class, Kind.PRIMARY, true, null, null);
    }
    
    public static ConfigKey<Character> charKey(Character defaultValue) {
    	return of(Character.class, Kind.PRIMARY, false, defaultValue, null);
    }
    
    public static <E extends Enum<E>> ConfigKey<E> enumKey(Class<E> enumClass) {
        return of(enumClass, Kind.PRIMARY, true, null, null);
    }
    
    public static <E extends Enum<E>> ConfigKey<E> enumKey(Class<E> enumClass, E defaultValue) {
        return of(enumClass, Kind.PRIMARY, false, defaultValue, null);
    }
    
    public static ConfigKey<String> stringKey() {
    	return of(String.class, Kind.PRIMARY, true, null, null);
    }
    
    public static ConfigKey<String> stringKey(String defaultValue) {
    	return of(String.class, Kind.PRIMARY, false, defaultValue, null);
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
    
    public boolean isRequired() {
    	return isRequired;
    }
    
    public T getDefaultValue() {
    	return defaultValue;
    }
    
    void setFieldName(String fieldName) {
    	this.fieldName = fieldName;
    	this.name = toCamelCase(fieldName);
    }
    
    private static String toCamelCase(String input) {
        if (input == null || input.isEmpty()) return input;

        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '_') {
                nextUpper = true;
            } else {
                if (sb.length() == 0) {
                    sb.append(Character.toLowerCase(c));
                } else if (nextUpper) {
                    sb.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    sb.append(Character.toLowerCase(c));
                }
            }
        }
        return sb.toString();
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
		
		String name = this.name == null ? "Not_Defined_Yet" : this.name;
		
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