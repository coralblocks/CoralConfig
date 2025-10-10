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

public class TcpClient extends Client {
	
	static enum SendStrategy {
		REGULAR, THROTTLED
	}
	
	public static final ConfigKey<SendStrategy> CLIENT_SEND_STRATEGY = enumKey(SendStrategy.class);
	
	public static final ConfigKey<Integer> MESSAGES_TO_SEND = intKey();

	public TcpClient(Configuration config) {
		
		super(overwrite(config));
		
		SendStrategy sendStrategy = config.get(CLIENT_SEND_STRATEGY, SendStrategy.REGULAR);
		
		int msgsToSend = config.get(MESSAGES_TO_SEND, 10);
		
		// Print what we got!
		
		System.out.println("Got => " + CLIENT_SEND_STRATEGY + " => " + sendStrategy);
		
		System.out.println("Got => " + MESSAGES_TO_SEND + " => " + msgsToSend);
	}
	
	private static Configuration overwrite(Configuration config) {
		config.overwriteDefault(MAX_RETRIES, 22);
		config.overwriteDefault(HEARTBEAT, 5.555f);
		return config;
	}
	
	public static void main(String[] args) {
		
		Configuration config = new MapConfiguration("username=saoj maxRetries=2 clientSendStrategy=Throttled", MoreConfigs.class, Client.class, TcpClient.class);
		
		System.out.println("-----> 1:");
		new TcpClient(config);
		System.out.println();
		
		MapConfiguration mapConfig = new MapConfiguration(MoreConfigs.class, Client.class, TcpClient.class);
		mapConfig.add(CLIENT_USERNAME, "rpaiva");
		mapConfig.add(SERVER_HOST, "192.168.1.1");
		mapConfig.add(MESSAGES_TO_SEND, 77);
		
		System.out.println("-----> 2:");
		new TcpClient(mapConfig);
		System.out.println();
		
		MapConfiguration mc1 = new MapConfiguration(MoreConfigs.class, Client.class, TcpClient.class);
		mc1.add(HEARTBEAT, 7.9f);
		
		System.out.println("-----> 3:");
		new TcpClient(mc1);
		System.out.println();
		
		mc1.add(HEARTBEAT_INTERVAL, 9);
		
		System.out.println("-----> 4:");
		new TcpClient(mc1);
		System.out.println();
		
		MapConfiguration mc2 = new MapConfiguration(mc1);
		mc2.remove(HEARTBEAT);
		
		System.out.println("-----> 5:");
		new TcpClient(mc2);
		System.out.println();
		
		MapConfiguration mc3 = new MapConfiguration(mc1);
		mc3.remove(HEARTBEAT_INTERVAL);
		
		System.out.println("-----> 6:");
		new TcpClient(mc3);
		System.out.println();
		
		System.out.println("-----> all configKeys:");
		for(ConfigKey<?> configKey : mapConfig.allConfigKeys()) {
			System.out.println(configKey);
		}
		
		System.out.println();
	}
}