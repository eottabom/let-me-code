package com.eottabom.letmecode.example.classloading.experiment.support;

import java.util.List;

/**
 * 중앙값, 최솟값, 최댓값.
 */
public final class Stats {

	private Stats() {
	}

	public static double median(List<Double> values) {
		List<Double> sorted = values.stream().sorted().toList();
		int middle = sorted.size() / 2;
		return (sorted.size() % 2 == 1) ? sorted.get(middle) : (sorted.get(middle - 1) + sorted.get(middle)) / 2;
	}

	public static double min(List<Double> values) {
		return values.stream().mapToDouble(Double::doubleValue).min().orElse(Double.NaN);
	}

	public static double max(List<Double> values) {
		return values.stream().mapToDouble(Double::doubleValue).max().orElse(Double.NaN);
	}

}
