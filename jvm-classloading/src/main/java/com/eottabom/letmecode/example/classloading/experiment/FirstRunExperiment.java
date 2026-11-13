package com.eottabom.letmecode.example.classloading.experiment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.eottabom.letmecode.example.classloading.experiment.support.ChildJvm;
import com.eottabom.letmecode.example.classloading.experiment.support.Report;
import com.eottabom.letmecode.example.classloading.experiment.support.Stats;
import com.eottabom.letmecode.example.classloading.target.FirstRun;

/**
 * 실험 3. 작업마다 1번째, 2번째 실행에서 로딩된 클래스 수와 실행 시간을 CDS 사용 여부에 따라 비교한다.
 * <ul>
 * <li>클래스 수: {@code -Xlog:class+load} 를 켜고 1번 실행해 센다.</li>
 * <li>시간: 로그 없이 첫 프로세스 1번과 그 뒤 {@code repeat} 번을 실행한다. 첫 프로세스는 따로 적고, 나머지로 중앙값을 낸다. 앞선 실험과 빌드가
 * 이미 JDK 파일을 읽었으므로 첫 프로세스도 디스크 캐시가 빈 상태는 아니다.</li>
 * </ul>
 * {@code -Xshare:off} 는 JDK 의 기본 CDS 아카이브를 읽지 않게 할 뿐 파일을 바꾸지 않는다.
 */
final class FirstRunExperiment {

	private static final List<String> TASKS = List.of("format", "regex", "datetime", "digest", "lambda");

	private static final Map<String, List<String>> CONDITIONS = new LinkedHashMap<>();

	private static final Pattern TIME = Pattern.compile("\\[time] first=(\\d+) second=(\\d+) tailMedian=(\\d+)");

	private static final double NANOS_PER_MICRO = 1_000.0;

	static {
		CONDITIONS.put("CDS 사용(기본)", List.of());
		CONDITIONS.put("CDS 끔", List.of("-Xshare:off"));
	}

	private FirstRunExperiment() {
	}

	static void run() {
		Report report = new Report("first-run");
		// 첫 프로세스가 이 실험에서 처음 뜨는 JVM 이 되도록 시간을 먼저 잰다.
		measureTime(report);
		countClasses(report);
	}

	private static void countClasses(Report report) {
		report.title("1번째, 2번째 실행에서 로딩된 클래스 수 (괄호 안은 CDS 아카이브에서 읽은 수)");
		report.print(String.format("%-10s  %-14s  %12s  %12s", "작업", "조건", "1번째", "2번째"));
		for (String task : TASKS) {
			for (Map.Entry<String, List<String>> condition : CONDITIONS.entrySet()) {
				List<String> options = new ArrayList<>(condition.getValue());
				options.add(ClassLog.LOG_OPTION);
				String output = ChildJvm.run(options, FirstRun.class, task, "count");
				report.saveRaw(task + "." + (condition.getValue().isEmpty() ? "cds" : "no-cds"), output);
				ClassLog log = new ClassLog(output);
				int[] first = log.between("1번째 실행 전", "1번째 실행 후").loadedCount();
				int[] second = log.between("1번째 실행 후", "2번째 실행 후").loadedCount();
				report.print(String.format("%-10s  %-14s  %12s  %12s", task, condition.getKey(),
						first[0] + " (" + first[1] + ")", second[0] + " (" + second[1] + ")"));
			}
		}
	}

	private static void measureTime(Report report) {
		int repeat = Report.repeat();
		Map<String, double[]> firstProcess = new LinkedHashMap<>();
		Map<String, List<double[]>> runs = new LinkedHashMap<>();
		// 시간이 흐르며 생기는 차이가 한 조건에 몰리지 않도록 작업과 조건을 번갈아 실행한다.
		for (int i = 0; i <= repeat; i++) {
			report.print("시간 측정 " + i + "/" + repeat + ((i == 0) ? " (첫 프로세스)" : ""));
			for (String task : TASKS) {
				for (Map.Entry<String, List<String>> condition : CONDITIONS.entrySet()) {
					String key = task + "\t" + condition.getKey();
					double[] values = parse(ChildJvm.run(condition.getValue(), FirstRun.class, task, "time"));
					if (i == 0) {
						firstProcess.put(key, values);
					}
					else {
						runs.computeIfAbsent(key, (k) -> new ArrayList<>()).add(values);
					}
				}
			}
		}
		report.title("실행 시간 (µs). 첫 프로세스 값, 그 뒤 " + repeat + "번의 중앙값 [최소 ~ 최대]");
		report.print(String.format("%-10s  %-14s  %10s  %26s  %26s  %26s", "작업", "조건", "첫 프로세스 1번째", "1번째",
				"2번째", "901~1000번째 중앙값"));
		runs.forEach((key, values) -> {
			String[] parts = key.split("\t");
			report.print(String.format("%-10s  %-14s  %10.1f  %26s  %26s  %26s", parts[0], parts[1],
					firstProcess.get(key)[0], summary(values, 0), summary(values, 1), summary(values, 2)));
		});
	}

	private static double[] parse(String output) {
		Matcher matcher = TIME.matcher(output);
		if (!matcher.find()) {
			throw new IllegalStateException("시간 줄이 없다\n" + output);
		}
		return new double[] { Long.parseLong(matcher.group(1)) / NANOS_PER_MICRO,
				Long.parseLong(matcher.group(2)) / NANOS_PER_MICRO, Long.parseLong(matcher.group(3)) / NANOS_PER_MICRO };
	}

	private static String summary(List<double[]> runs, int index) {
		List<Double> values = runs.stream().map((value) -> value[index]).toList();
		return String.format("%.1f [%.1f ~ %.1f]", Stats.median(values), Stats.min(values), Stats.max(values));
	}

}
