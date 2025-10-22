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

/**
 * A <code>DeprecatedListener</code> can receive callbacks from a <code>Configuration</code> when a deprecated <code>ConfigKey</code> is accessed. 
 */
public interface DeprecatedListener {
	
	/**
	 * The given deprecated <code>ConfigKey</code> was accessed. The default implementation of this method simply prints information to stdout.
	 * 
	 * @param deprecatedKey the deprecated <code>ConfigKey</code> accessed
	 * @param primaryKey the primary <code>ConfigKey</code> of the accessed deprecated <code>ConfigKey</code>
	 */
	default public void deprecatedConfig(ConfigKey<?> deprecatedKey, ConfigKey<?> primaryKey) {
		
		System.out.println("---CoralConfig---> You are using a deprecated config key!" +
				" holder=" + primaryKey.getHolder().getName() +
				" deprecatedKey=" + deprecatedKey.getFieldName() + "(\"" + deprecatedKey.getParamName() + "\")" +
				" inFavorOf=" + primaryKey.getFieldName() + "(\"" + primaryKey.getParamName() + "\")");
	}
}