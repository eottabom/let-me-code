package com.eottabom.letmecode.example.classloading.experiment;

import java.util.List;

import com.eottabom.letmecode.example.classloading.experiment.support.ChildJvm;
import com.eottabom.letmecode.example.classloading.experiment.support.Report;
import com.eottabom.letmecode.example.classloading.target.LoadTiming;

/**
 * 실험 1. 분기를 실행하지 않을 때와 실행할 때, {@code LoadTiming} 의 중첩 클래스가 언제 로딩되는지 로그 순서로 본다.
 * <p>
 * JVMS 는 로딩 시점을 구현에 맡기므로, 이 결과는 실험한 HotSpot 에서 관찰한 순서다.
 */
final class LoadTimingExperiment {

	private static final String FILTER = "LoadTiming$";

	private LoadTimingExperiment() {
	}

	static void run() {
		Report report = new Report("load-timing");
		trace(report, "분기 실행 안 함", "no-branch");
		trace(report, "분기 실행", "branch");
	}

	private static void trace(Report report, String title, String argument) {
		report.title(title);
		String output = ChildJvm.run(List.of(ClassLog.LOG_OPTION), LoadTiming.class, argument);
		report.saveRaw(argument, output);
		ClassLog log = new ClassLog(output);
		log.trace(FILTER).forEach(report::print);
		report.print("로딩된 중첩 클래스: " + ClassLog.simpleNames(log.loaded(FILTER)));
	}

}
