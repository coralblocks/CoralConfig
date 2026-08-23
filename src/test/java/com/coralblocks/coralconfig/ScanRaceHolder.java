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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.coralblocks.coralconfig;

import static com.coralblocks.coralconfig.ConfigKey.intKey;

/**
 * Holder used by <code>ThreadSafetyTest.testConcurrentFirstHolderScan</code>. Each test round defines this class
 * again through a fresh child-first class loader, producing a brand-new <code>Class</code> object, so every round
 * races the very first <code>ConfigContainer</code> scan instead of hitting the cached container.
 */
public class ScanRaceHolder {

	public static final ConfigKey<Integer> PRIMARY = intKey(1);
	public static final ConfigKey<Integer> ALIAS = intKey().alias(PRIMARY);
	public static final ConfigKey<Integer> DEPRECATED = intKey().deprecated(PRIMARY);
}
