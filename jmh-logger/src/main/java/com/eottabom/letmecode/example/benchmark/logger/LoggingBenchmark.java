package com.eottabom.letmecode.example.benchmark.logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.slf4j.LoggerFactory;

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 5, time = 3)
@Measurement(iterations = 10, time = 3)
public class LoggingBenchmark {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(LoggingBenchmark.class);

	private static final java.util.logging.Logger javaLogger = java.util.logging.Logger
		.getLogger(LoggingBenchmark.class.getSimpleName());

	private static final int THREAD_COUNT = 4;

	private static final int ITERATIONS_PER_THREAD = 1000;

	@Benchmark
	public void benchmarkSystemOutPrintln() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
		for (int i = 0; i < THREAD_COUNT; i++) {
			executor.submit(() -> {
				for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
					System.out.println("Test message " + j);
				}
			});
		}
		executor.shutdown();
		executor.awaitTermination(1, TimeUnit.HOURS);
	}

	@Benchmark
	public void benchmarkLoggerEnabled() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
		logger.setLevel(Level.INFO);
		for (int i = 0; i < THREAD_COUNT; i++) {
			executor.submit(() -> {
				for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
					logger.info("Test message " + j);
				}
			});
		}
		executor.shutdown();
		executor.awaitTermination(1, TimeUnit.HOURS);
	}

	@Benchmark
	public void benchmarkLoggerDisabled() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
		logger.setLevel(Level.OFF);
		for (int i = 0; i < THREAD_COUNT; i++) {
			executor.submit(() -> {
				for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
					logger.info("Test message " + j);
				}
			});
		}
		executor.shutdown();
		executor.awaitTermination(1, TimeUnit.HOURS);
	}

	@Benchmark
	public void benchmarkJavaLoggerEnabled() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
		for (int i = 0; i < THREAD_COUNT; i++) {
			executor.submit(() -> {
				for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
					javaLogger.info("Test message " + j);
				}
			});
		}
		executor.shutdown();
		executor.awaitTermination(1, TimeUnit.HOURS);
	}

	@Benchmark
	public void benchmarkJavaLoggerDisabled() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
		javaLogger.setLevel(java.util.logging.Level.OFF);
		for (int i = 0; i < THREAD_COUNT; i++) {
			executor.submit(() -> {
				for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
					javaLogger.info("Test message " + j);
				}
			});
		}
		executor.shutdown();
		executor.awaitTermination(1, TimeUnit.HOURS);
	}

}
