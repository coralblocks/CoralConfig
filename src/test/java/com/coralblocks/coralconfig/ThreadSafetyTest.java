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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

public class ThreadSafetyTest {

	@Test(timeout = 10000)
	public void testConcurrentFirstHolderScan() throws Exception {

		class Holder {

			public static final ConfigKey<Integer> PRIMARY = intKey(1);
			public static final ConfigKey<Integer> ALIAS = intKey().alias(PRIMARY);
			public static final ConfigKey<Integer> DEPRECATED = intKey().deprecated(PRIMARY);
		}

		int threadCount = 16;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch ready = new CountDownLatch(threadCount);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<ConfigContainer>> futures = new ArrayList<Future<ConfigContainer>>();

		try {
			for(int i = 0; i < threadCount; i++) {
				futures.add(executor.submit(() -> {
					ready.countDown();
					start.await();
					return ConfigContainer.of(Holder.class);
				}));
			}

			Assert.assertTrue(ready.await(5, TimeUnit.SECONDS));
			start.countDown();

			ConfigContainer expected = futures.get(0).get();
			for(Future<ConfigContainer> future : futures) {
				Assert.assertSame(expected, future.get());
			}
		} finally {
			executor.shutdownNow();
		}

		Assert.assertEquals(1, Holder.PRIMARY.getAliases().size());
		Assert.assertSame(Holder.ALIAS, Holder.PRIMARY.getAliases().get(0));
		Assert.assertEquals(1, Holder.PRIMARY.getDeprecated().size());
		Assert.assertSame(Holder.DEPRECATED, Holder.PRIMARY.getDeprecated().get(0));
	}

	@Test(timeout = 10000)
	public void testHolderCanCreateConfigurationDuringInitialization() {

		class Holder {

			public static final ConfigKey<Integer> VALUE = intKey(1);
			public static final MapConfiguration CONFIG = new MapConfiguration(Holder.class);
		}

		Assert.assertEquals(1, Holder.CONFIG.get(Holder.VALUE).intValue());
	}

	@Test(timeout = 15000)
	public void testReadsAndCopiesRemainConsistentDuringWrites() throws Exception {

		class Holder {

			public static final ConfigKey<Integer> VALUE = intKey(1);
		}

		MapConfiguration config = new MapConfiguration(Holder.class);
		int readerCount = 4;
		int taskCount = readerCount + 2;
		int iterations = 20000;
		ExecutorService executor = Executors.newFixedThreadPool(taskCount);
		CountDownLatch ready = new CountDownLatch(taskCount);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<Future<?>>();

		try {
			futures.add(executor.submit(() -> {
				awaitStart(ready, start);
				for(int i = 0; i < iterations; i++) {
					config.overwriteDefault(Holder.VALUE, 2);
					config.removeOverwrittenDefault(Holder.VALUE);
				}
			}));

			for(int i = 0; i < readerCount; i++) {
				futures.add(executor.submit(() -> {
					awaitStart(ready, start);
					for(int j = 0; j < iterations; j++) {
						assertDeclaredOrOverwritten(config.get(Holder.VALUE));
					}
				}));
			}

			futures.add(executor.submit(() -> {
				awaitStart(ready, start);
				for(int i = 0; i < iterations / 10; i++) {
					MapConfiguration copy = new MapConfiguration(config);
					assertDeclaredOrOverwritten(copy.get(Holder.VALUE));
				}
			}));

			Assert.assertTrue(ready.await(5, TimeUnit.SECONDS));
			start.countDown();
			for(Future<?> future : futures) future.get();
		} finally {
			executor.shutdownNow();
		}
	}

	@Test(timeout = 10000)
	public void testDeprecatedKeyIsReportedOnceUnderContention() throws Exception {

		class Holder {

			public static final ConfigKey<Integer> PRIMARY = intKey(1);
			public static final ConfigKey<Integer> DEPRECATED = intKey().deprecated(PRIMARY);
		}

		MapConfiguration config = new MapConfiguration(Holder.class);
		AtomicInteger calls = new AtomicInteger();
		config.addListener(new DeprecatedListener() {

			@Override
			public void deprecatedConfig(ConfigKey<?> deprecatedKey, ConfigKey<?> primaryKey) {
				calls.incrementAndGet();
			}
		});

		int threadCount = 16;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch ready = new CountDownLatch(threadCount);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<Future<?>>();

		try {
			for(int i = 0; i < threadCount; i++) {
				futures.add(executor.submit(() -> {
					awaitStart(ready, start);
					Assert.assertEquals(1, config.get(Holder.DEPRECATED).intValue());
				}));
			}

			Assert.assertTrue(ready.await(5, TimeUnit.SECONDS));
			start.countDown();
			for(Future<?> future : futures) future.get();
		} finally {
			executor.shutdownNow();
		}

		Assert.assertEquals(1, calls.get());
	}

	private static void awaitStart(CountDownLatch ready, CountDownLatch start) {
		ready.countDown();
		try {
			start.await();
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		}
	}

	private static void assertDeclaredOrOverwritten(Integer value) {
		if (value == null || (value.intValue() != 1 && value.intValue() != 2)) {
			throw new AssertionError("Unexpected value: " + value);
		}
	}
}
