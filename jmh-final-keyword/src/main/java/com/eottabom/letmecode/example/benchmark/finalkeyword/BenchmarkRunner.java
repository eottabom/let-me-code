package com.eottabom.letmecode.example.benchmark.finalkeyword;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

public class BenchmarkRunner {

	public static void main(String[] args) throws IOException {
		String[] jmhArgs = {
				"-f", "1", // forks
				"-wi", "5", // warmup iterations
				"-i", "5", // Measurements iterations
				"-t", "1", // Threads
		};
		org.openjdk.jmh.Main.main(jmhArgs);
	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	@BenchmarkMode(Mode.AverageTime)
	public static String nonFinalStrings() {
		String x = "x";
		String y = "y";
		return x + y;
	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	@BenchmarkMode(Mode.AverageTime)
	public static String compilerOptimizationString() {
		return "xy";
	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	@BenchmarkMode(Mode.AverageTime)
	public static String finalStrings() {
		final String x = "x";
		final String y = "y";
		return x + y;
	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	@BenchmarkMode(Mode.AverageTime)
	public static int nonFinalMethodBenchmark() {
		MethodTest test = new MethodTest();
		return test.nonFinalMethod();
	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	@BenchmarkMode(Mode.AverageTime)
	public static int finalMethodBenchmark() {
		MethodTest test = new MethodTest();
		return test.finalMethod();
	}

}