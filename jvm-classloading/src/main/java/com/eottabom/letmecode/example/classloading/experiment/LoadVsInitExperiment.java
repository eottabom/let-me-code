package com.eottabom.letmecode.example.classloading.experiment;

import java.util.List;

import com.eottabom.letmecode.example.classloading.experiment.support.ChildJvm;
import com.eottabom.letmecode.example.classloading.experiment.support.Report;
import com.eottabom.letmecode.example.classloading.target.LoadVsInit;

/**
 * 실험 2. 참조 방식마다 JVM 을 새로 띄워, 표시 줄 사이에서 로딩된 클래스와 초기화된 클래스를 비교한다.
 */
final class LoadVsInitExperiment {

	private static final String FILTER = "LoadVsInit$";

	private static final List<String> CASES = List.of("forName-no-init", "constant-variable", "static-final-boxed",
			"array", "subclass-static-field", "static-method");

	private LoadVsInitExperiment() {
	}

	static void run() {
		Report report = new Report("load-vs-init");
		report.title("경우별 로딩과 초기화 (실행 전 표시와 실행 후 표시 사이)");
		report.print(String.format("%-22s  %-30s  %s", "경우", "로딩", "초기화"));
		for (String name : CASES) {
			String output = ChildJvm.run(List.of(ClassLog.LOG_OPTION), LoadVsInit.class, name);
			report.saveRaw(name, output);
			ClassLog during = new ClassLog(output).between("실행 전", "실행 후");
			report.print(String.format("%-22s  %-30s  %s", name, ClassLog.simpleNames(during.loaded(FILTER)),
					ClassLog.simpleNames(during.initialized(FILTER))));
		}
	}

}
