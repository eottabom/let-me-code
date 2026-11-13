package com.eottabom.letmecode.example.classloading.experiment;

import java.util.List;

import com.eottabom.letmecode.example.classloading.experiment.support.ChildJvm;
import com.eottabom.letmecode.example.classloading.experiment.support.Report;
import com.eottabom.letmecode.example.classloading.target.InitFailure;

/**
 * 실험 4. static 초기화에서 {@link RuntimeException} 이 났을 때 1번째와 2번째 접근의 예외를 본다.
 */
final class InitFailureExperiment {

	private InitFailureExperiment() {
	}

	static void run() {
		Report report = new Report("init-failure");
		report.title("같은 클래스에 두 번 접근");
		String output = ChildJvm.run(List.of(ClassLog.LOG_OPTION), InitFailure.class);
		report.saveRaw("trace", output);
		// JVM 로그를 뺀 프로그램 출력만 남긴다.
		output.lines().filter((line) -> !line.startsWith("[0")).forEach(report::print);
		report.title("초기화 로그");
		new ClassLog(output).trace("InitFailure$Config")
			.stream()
			.filter((line) -> line.startsWith("[0"))
			.forEach(report::print);
	}

}
