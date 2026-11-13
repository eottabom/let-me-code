package com.eottabom.letmecode.example.classloading.experiment;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 실험 진입점. 첫 인자로 실험 이름을 받는다. {@code all} 은 모든 실험을 차례로 실행한다.
 *
 * <pre>
 * ./gradlew :jvm-classloading:run --args='load-timing'
 * ./gradlew :jvm-classloading:run --args='first-run' -Prepeat=3
 * ./gradlew :jvm-classloading:run --args='all'
 * </pre>
 */
public final class Experiments {

	private static final Logger log = LoggerFactory.getLogger(Experiments.class);

	private static final Map<String, Runnable> EXPERIMENTS = new LinkedHashMap<>();

	static {
		EXPERIMENTS.put("load-timing", LoadTimingExperiment::run);
		EXPERIMENTS.put("load-vs-init", LoadVsInitExperiment::run);
		EXPERIMENTS.put("first-run", FirstRunExperiment::run);
		EXPERIMENTS.put("init-failure", InitFailureExperiment::run);
	}

	private Experiments() {
	}

	public static void main(String[] args) {
		String name = (args.length > 0) ? args[0] : "";
		if ("all".equals(name)) {
			EXPERIMENTS.values().forEach(Runnable::run);
			return;
		}
		Runnable experiment = EXPERIMENTS.get(name);
		if (experiment == null) {
			log.error("실험 이름을 넘겨야 한다: {}, all", EXPERIMENTS.keySet());
			System.exit(1);
		}
		experiment.run();
	}

}
