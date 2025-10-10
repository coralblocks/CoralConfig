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

import static com.coralblocks.coralconfig.ConfigKey.*;

import com.coralblocks.coralconfig.ConfigKey;
import com.coralblocks.coralconfig.Configuration;
import com.coralblocks.coralconfig.MapConfiguration;

public class Client implements MoreConfigs {

	public static final ConfigKey<Integer> HEARTBEAT_INTERVAL = intKey("heartbeatInterval");
	public static final ConfigKey<Float> HEARTBEAT = floatKeyDeprecated("heartbeat", HEARTBEAT_INTERVAL);
	
	public static final ConfigKey<String> CLIENT_USERNAME = stringKey("clientUsername");
	public static final ConfigKey<String> USERNAME = stringKeyAlias("username", CLIENT_USERNAME);
	
	public Client(Configuration config) {
		
		int maxRetries = config.get(MAX_RETRIES, 10); // from MoreConfigs
		
		int heartbeatInterval = config.get(HEARTBEAT_INTERVAL, 5);
		
		float heartbeat = config.get(HEARTBEAT, 4.444f);
		
		String clientUsername = config.get(CLIENT_USERNAME, "testClient");
		
		String serverIp = config.get(SERVER_IP, "localhost"); // using alias from MoreConfigs
		
		// Print what we got!
		
		System.out.println("Got => " + MAX_RETRIES + " => " + maxRetries);
		
		System.out.println("Got => " + HEARTBEAT_INTERVAL + " => " + heartbeatInterval);
		
		System.out.println("Got => " + HEARTBEAT + " => " + heartbeat);
		
		System.out.println("Got => " + CLIENT_USERNAME + " => " + clientUsername);
		
		System.out.println("Got => " + SERVER_IP + " => " + serverIp);
		
		System.out.println();
		
	}
	
	public static void main(String[] args) {
		
		Configuration config = new MapConfiguration("username=saoj maxRetries=2", MoreConfigs.class, Client.class);
		
		System.out.println("-----> 1:");
		new Client(config);
		
		MapConfiguration mapConfig = new MapConfiguration(MoreConfigs.class, Client.class);
		mapConfig.add(CLIENT_USERNAME, "rpaiva");
		mapConfig.add(SERVER_HOST, "192.168.1.1");
		
		System.out.println("-----> 2:");
		new Client(mapConfig);
		
		MapConfiguration mc1 = new MapConfiguration(MoreConfigs.class, Client.class);
		mc1.add(HEARTBEAT, 7.9f);
		
		System.out.println("-----> 3:");
		new Client(mc1);
		
		mc1.add(HEARTBEAT_INTERVAL, 9);
		
		System.out.println("-----> 4:");
		new Client(mc1);
		
		MapConfiguration mc2 = new MapConfiguration(mc1);
		mc2.remove(HEARTBEAT);
		
		System.out.println("-----> 5:");
		new Client(mc2);
		
		MapConfiguration mc3 = new MapConfiguration(mc1);
		mc3.remove(HEARTBEAT_INTERVAL);
		
		System.out.println("-----> 6:");
		new Client(mc3);
		
		System.out.println("-----> all configKeys:");
		for(ConfigKey<?> configKey : mapConfig.allConfigKeys()) {
			System.out.println(configKey);
		}
	}
	
}