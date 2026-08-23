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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

public class ThreadSafetyTest {

	@Test(timeout = 30000)
	public void testConcurrentFirstHolderScan() throws Exception {

		// Each round defines ScanRaceHolder again through a fresh child-first class loader, producing a
		// brand-new Class object, so every round races the very first ConfigContainer scan of that class
		// instead of hitting the container cached by an earlier round.

		int threadCount = 16;
		int rounds = 300;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);

		try {
			for(int round = 0; round < rounds; round++) {

				final Class<?> holder = loadFreshHolderClass();
				Assert.assertNotSame(ScanRaceHolder.class, holder); // must be a fresh Class, or the round tests nothing
				final CyclicBarrier barrier = new CyclicBarrier(threadCount);
				List<Future<ConfigContainer>> futures = new ArrayList<Future<ConfigContainer>>();

				for(int i = 0; i < threadCount; i++) {
					futures.add(executor.submit(() -> {
						barrier.await();
						return ConfigContainer.of(holder);
					}));
				}

				ConfigContainer expected = futures.get(0).get();
				for(Future<ConfigContainer> future : futures) {
					Assert.assertSame(expected, future.get());
				}

				ConfigKey<?> primary = (ConfigKey<?>) holder.getField("PRIMARY").get(null);
				ConfigKey<?> alias = (ConfigKey<?>) holder.getField("ALIAS").get(null);
				ConfigKey<?> deprecated = (ConfigKey<?>) holder.getField("DEPRECATED").get(null);

				Assert.assertEquals(1, primary.getAliases().size());
				Assert.assertSame(alias, primary.getAliases().get(0));
				Assert.assertEquals(1, primary.getDeprecated().size());
				Assert.assertSame(deprecated, primary.getDeprecated().get(0));
			}
		} finally {
			executor.shutdownNow();
		}
	}

	private static Class<?> loadFreshHolderClass() throws Exception {

		ClassLoader loader = new ClassLoader(ThreadSafetyTest.class.getClassLoader()) {

			@Override
			protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
				if (!name.equals(ScanRaceHolder.class.getName())) return super.loadClass(name, resolve);
				synchronized(getClassLoadingLock(name)) {
					Class<?> defined = findLoadedClass(name);
					if (defined == null) {
						String resource = name.replace('.', '/') + ".class";
						try(InputStream in = getParent().getResourceAsStream(resource)) {
							if (in == null) throw new ClassNotFoundException(name);
							byte[] bytes = in.readAllBytes();
							defined = defineClass(name, bytes, 0, bytes.length);
						} catch(IOException e) {
							throw new ClassNotFoundException(name, e);
						}
					}
					if (resolve) resolveClass(defined);
					return defined;
				}
			}
		};

		return Class.forName(ScanRaceHolder.class.getName(), false, loader);
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
