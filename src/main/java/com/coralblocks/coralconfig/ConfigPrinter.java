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

/**
 * Command-line utility for printing the <code>ConfigKey</code>s declared by holder classes as CSV.
 * Fields containing commas, quotes, or line breaks are quoted, and quotes inside fields are doubled.
 *
 * The first three arguments control whether the header, parameter name, and holder class columns are included.
 * Each flag can be passed as a bare boolean or in <code>name=value</code> form. All remaining arguments must be
 * fully qualified holder class names.
 */
public final class ConfigPrinter {
	
	private ConfigPrinter() {
		
	}
	
	/**
	 * Prints configuration metadata as CSV.
	 *
	 * @param args three boolean flags followed by one or more fully qualified holder class names
	 * @throws IllegalArgumentException if a boolean flag is not <code>true</code> or <code>false</code>
	 * @throws RuntimeException if a holder class cannot be loaded
	 */
	public static void main(String[] args) {
		
		if (args.length <= 3) {
			System.out.println("Missing arguments: includeHeaderLine=true|false"
								+ " includeParamName=true|false"
								+ " includeHolderClass=true|false"
								+ " Holder1 Holder2 ...\n");
			return;
		}
		
		boolean includeHeaderLine;
		String arg1 = args[0];
		if (arg1.contains("=")) {
			if (!arg1.startsWith("includeHeaderLine=")) {
				System.out.println("First argument must be includeHeaderLine=true|false\n");
				return;
			}
			includeHeaderLine = parseBoolean(arg1.substring(arg1.indexOf('=') + 1), "includeHeaderLine");
		} else {
			includeHeaderLine = parseBoolean(arg1, "includeHeaderLine");
		}
		
		boolean includeParamName;
		String arg2 = args[1];
		if (arg2.contains("=")) {
			if (!arg2.startsWith("includeParamName=")) {
				System.out.println("Second argument must be includeParamName=true|false\n");
				return;
			}
			includeParamName = parseBoolean(arg2.substring(arg2.indexOf('=') + 1), "includeParamName");
		} else {
			includeParamName = parseBoolean(arg2, "includeParamName");
		}
		
		boolean includeHolderClass;
		String arg3 = args[2];
		if (arg3.contains("=")) {
			if (!arg3.startsWith("includeHolderClass=")) {
				System.out.println("Third argument must be includeHolderClass=true|false\n");
				return;
			}
			includeHolderClass = parseBoolean(arg3.substring(arg3.indexOf('=') + 1), "includeHolderClass");
		} else {
			includeHolderClass = parseBoolean(arg3, "includeHolderClass");
		}
		
		Class<?>[] classArray = new Class<?>[args.length - 3];
		
		for(int i = 3; i < args.length; i++) {
			String className = args[i];
			try {
				classArray[i - 3] = Class.forName(className);
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		printConfigs(includeHeaderLine, includeParamName, includeHolderClass, classArray);
	}

	private static boolean parseBoolean(String value, String argumentName) {
		if ("true".equalsIgnoreCase(value)) return true;
		if ("false".equalsIgnoreCase(value)) return false;
		throw new IllegalArgumentException("Invalid boolean value for " + argumentName + ": " + value);
	}
	
	/**
	 * Prints configuration metadata as CSV.
	 *
	 * @param includeHeaderLine whether to print a header row
	 * @param includeParamName whether to include parameter names
	 * @param includeHolderClass whether to include holder class names
	 * @param holders holder classes declaring the <code>ConfigKey</code>s to print
	 */
	public static void printConfigs(final boolean includeHeaderLine,
									final boolean includeParamName,
									final boolean includeHolderClass,
									Class<?> ... holders) {
		
		MapConfiguration mc = new MapConfiguration(holders);
		
		List<ConfigKey<?>> allConfigs = mc.allConfigKeys();
		
		List<ConfigKey<?>> sorted = new ArrayList<ConfigKey<?>>(allConfigs);
		sorted.sort(Comparator.comparing(ConfigKey::getFieldName));

		List<String> header = new ArrayList<String>();
		header.add("Field Name");
		if (includeParamName) header.add("Param Name");
		header.add("Type");
		header.add("Default Value");
		if (includeHolderClass) header.add("Holder Class");
		header.add("Kind");
		header.add("Parent Primary");
		header.add("Aliases");
		header.add("Deprecated");
		header.add("Description");

		if (includeHeaderLine) System.out.println(toCsvLine(header));

		for(final ConfigKey<?> key : sorted) {
			List<String> fields = new ArrayList<String>();
			fields.add(key.getFieldName());
			if (includeParamName) fields.add(key.getParamName());
			fields.add(key.getType().isEnum() ? "Enum" : key.getType().getSimpleName());
			fields.add(key.hasDefault() ? String.valueOf(key.getDefaultValue()) : "=REQUIRED=");
			if (includeHolderClass) fields.add(key.getHolder().getSimpleName());
			fields.add(key.getKind().toString());
			fields.add(key.getKind() == Kind.PRIMARY ? "" : key.getPrimary().getFieldName());
			fields.add(joinFieldNames(key.getAliases()));
			fields.add(joinFieldNames(key.getDeprecated()));
			fields.add(key.getDescription() == null ? "" : key.getDescription());
			System.out.println(toCsvLine(fields));
		}
	}

	private static String joinFieldNames(List<ConfigKey<?>> configKeys) {
		StringBuilder joined = new StringBuilder();
		for(ConfigKey<?> configKey : configKeys) {
			if (joined.length() > 0) joined.append(';');
			joined.append(configKey.getFieldName());
		}
		return joined.toString();
	}

	private static String toCsvLine(List<String> fields) {
		StringBuilder line = new StringBuilder();
		for(int i = 0; i < fields.size(); i++) {
			if (i > 0) line.append(',');
			line.append(escapeCsvField(fields.get(i)));
		}
		return line.toString();
	}

	private static String escapeCsvField(String field) {
		if (field.indexOf(',') < 0 && field.indexOf('"') < 0 && field.indexOf('\n') < 0 && field.indexOf('\r') < 0) {
			return field;
		}
		return "\"" + field.replace("\"", "\"\"") + "\"";
	}
}
