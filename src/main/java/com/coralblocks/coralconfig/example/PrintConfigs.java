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
package com.coralblocks.coralconfig.example;

import com.coralblocks.coralconfig.ConfigPrinter;

public class PrintConfigs {
	
	public static void main(String[] args) {
		
		boolean withHeaderLine = args.length > 0 ? Boolean.parseBoolean(args[0]) : true;
		boolean fullHolderName = args.length > 1 ? Boolean.parseBoolean(args[1]) : false;
		
		ConfigPrinter.printConfigs(withHeaderLine, fullHolderName, TcpClient.class, Client.class);
	}
}