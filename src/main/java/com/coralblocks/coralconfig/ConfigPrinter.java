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
import java.util.Comparator;
import java.util.List;

import com.coralblocks.coralconfig.ConfigKey.Kind;

public class ConfigPrinter {
	
	private ConfigPrinter() {
		
	}
	
	public static void main(String[] args) {
		
		if (args.length <= 2) {
			System.out.println("Missing arguments: withHeaderLine=true|false fullHolderName=true|false Hoder1 Hoder2 ...\n");
			return;
		}
		
		boolean withHeaderLine = true;
		String arg1 = args[0];
		if (arg1.contains("=")) {
			if (!arg1.startsWith("withHeaderLine=")) {
				System.out.println("First argument must be withHeaderLine=true|false\n");
				return;
			}
			withHeaderLine = Boolean.parseBoolean(arg1.split("\\=")[1]);
		} else {
			withHeaderLine = Boolean.parseBoolean(arg1);
		}
		
		boolean fullHolderName = true;
		String arg2 = args[1];
		if (arg2.contains("=")) {
			if (!arg1.startsWith("fullHolderName=")) {
				System.out.println("First argument must be fullHolderName=true|false\n");
				return;
			}
			fullHolderName = Boolean.parseBoolean(arg2.split("\\=")[1]);
		} else {
			fullHolderName = Boolean.parseBoolean(arg2);
		}
		
		Class<?>[] classArray = new Class<?>[args.length - 2];
		
		for(int i = 2; i < args.length; i++) {
			String className = args[i];
			try {
				classArray[i - 2] = Class.forName(className);
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		printConfigs(withHeaderLine, fullHolderName, classArray);
	}
	
	public static final void printConfigs(final boolean withHeaderLine, final boolean fullHolderName, Class<?> ... holders) {
		
		MapConfiguration mc = new MapConfiguration(holders);
		
		List<ConfigKey<?>> allConfigs = mc.allConfigKeys();
		
        Comparator<ConfigKey<?>> byName = new Comparator<ConfigKey<?>>() {
            @Override
            public int compare(ConfigKey<?> ck1, ConfigKey<?> ck2) {
            	return ck1.getFieldName().compareTo(ck2.getFieldName());
            }
        };
        
        List<ConfigKey<?>> sorted = new ArrayList<ConfigKey<?>>(allConfigs);
        sorted.sort(byName);
        
        String header = "Field Name, Name, Type, Default Value, Holder Class, Kind, Parent Primary, Aliases, Deprecated, Description";
        
        if (withHeaderLine) System.out.println(header);
        
        for(final ConfigKey<?> key : sorted) {
        	String line = "";
        	line += key.getFieldName();
        	line += ", " + key.getName();
        	if (key.getType().isEnum()) {
        		line += ", Enum";
        	} else {
        		line += ", " + key.getType().getSimpleName();
        	}
        	if (key.hasDefault()) {
    			line += ", " + key.getDefaultValue();
        	} else {
        		line += ", =REQUIRED=";
        	}
        	line += ", " + (fullHolderName ? key.getHolder().getName() : key.getHolder().getSimpleName());
        	line += ", " + key.getKind();
        	if (key.getKind() == Kind.PRIMARY) {
        		line += ", ";
        	} else {
        		line += ", " + key.getPrimary().getFieldName();
        	}
        	if (key.getKind() != Kind.PRIMARY) {
        		line += ", ";
        	} else {
        		StringBuilder sb = new StringBuilder();
        		for(ConfigKey<?> ck : key.getAliases()) {
        			if (sb.length() > 0) sb.append(';');
        			sb.append(ck.getFieldName());
        		}
        		if (sb.length() == 0) {
        			line += ", ";
        		} else {
        			line += ", " + sb.toString();
        		}
        	}
        	if (key.getKind() != Kind.PRIMARY) {
        		line += ", ";
        	} else {
        		StringBuilder sb = new StringBuilder();
        		for(ConfigKey<?> ck : key.getDeprecated()) {
        			if (sb.length() > 0) sb.append(';');
        			sb.append(ck.getFieldName());
        		}
        		if (sb.length() == 0) {
        			line += ", ";
        		} else {
        			line += ", " + sb.toString();
        		}
        	}
        	if (key.getDescription() != null) {
        		line += ", " + key.getDescription();
        	} else {
        		line += ", ";
        	}
        	System.out.println(line);
        }
	}
}