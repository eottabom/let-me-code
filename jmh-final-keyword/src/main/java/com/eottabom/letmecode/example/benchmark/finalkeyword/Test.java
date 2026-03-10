package com.eottabom.letmecode.example.benchmark.finalkeyword;

public class Test {

	public static String nonFinalStrings() {
		String x = "x";
		String y = "y";
		return x + y;
	}

	public static String compilerOptimizationString() {
		return "xy";
	}

	public static String finalStrings() {
		final String x = "x";
		final String y = "y";
		return x + y;
	}

}
