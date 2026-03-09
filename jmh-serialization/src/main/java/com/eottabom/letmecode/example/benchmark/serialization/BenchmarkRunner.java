package com.eottabom.letmecode.example.benchmark.serialization;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public final class BenchmarkRunner {

	private BenchmarkRunner() {
	}

	public static void main(String[] args) throws Exception {
		Options opt = new OptionsBuilder().include(SerializationBenchmark.class.getSimpleName()).forks(1).build();

		new Runner(opt).run();
	}

}
