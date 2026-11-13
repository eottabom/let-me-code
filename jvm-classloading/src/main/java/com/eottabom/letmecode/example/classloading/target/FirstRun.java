package com.eottabom.letmecode.example.classloading.target;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 실험 3. 같은 코드를 처음 실행할 때와 반복해서 실행할 때를 비교한다. 첫 인자로 작업을, 두 번째 인자로 모드를 고른다.
 * <ul>
 * <li>{@code time}: 1번째, 2번째 실행 시간과 901 ~ 1,000번째 실행 시간의 중앙값을 나노초로 찍는다. 로그 출력이 시간을 늘리지 않도록
 * {@code -Xlog} 없이 띄운다.</li>
 * <li>{@code count}: 1번째, 2번째 실행 앞뒤에 표시 줄만 찍는다. {@code -Xlog:class+load} 와 함께 띄워 그 사이에 로딩된 클래스를 센다.</li>
 * </ul>
 * 첫 실행 시간에는 클래스 로딩 말고도 객체 생성, 내부 캐시 초기화가 함께 들어 있고, 반복 실행 시간에는 JIT 컴파일 효과도 섞여 있다. 그래서 로딩된 클래스 수와
 * 시간을 따로 기록하고, 클래스 수만으로 시간의 원인을 정하지 않는다.
 */
public final class FirstRun {

	private static final int ITERATIONS = 1_000;

	private static final int TAIL = 100;

	private static final byte[] INPUT = "order-42".getBytes(StandardCharsets.UTF_8);

	private FirstRun() {
	}

	public static void main(String[] args) throws NoSuchAlgorithmException {
		String task = args[0];
		if ("count".equals(args[1])) {
			Marker.mark("[mark] 1번째 실행 전");
			runTask(task, 0);
			Marker.mark("[mark] 1번째 실행 후");
			runTask(task, 1);
			Marker.mark("[mark] 2번째 실행 후");
			return;
		}
		long[] elapsed = new long[ITERATIONS];
		for (int i = 0; i < ITERATIONS; i++) {
			long start = System.nanoTime();
			runTask(task, i);
			elapsed[i] = System.nanoTime() - start;
		}
		long[] tail = Arrays.copyOfRange(elapsed, ITERATIONS - TAIL, ITERATIONS);
		Arrays.sort(tail);
		System.out.println("[time] first=" + elapsed[0] + " second=" + elapsed[1] + " tailMedian=" + tail[TAIL / 2]);
	}

	// 람다로 작업을 고르면 람다를 처음 만드는 비용이 모든 작업의 1번째 실행에 섞이므로 switch 로 나눈다.
	private static Object runTask(String task, int i) throws NoSuchAlgorithmException {
		return switch (task) {
			case "format" -> String.format("%s-%d", "order", i);
			case "regex" -> Pattern.compile("([a-z]+)-(\\d+)").matcher("order-42").matches();
			case "datetime" -> DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(LocalDateTime.of(2026, 11, 10, 9, i % 60));
			case "digest" -> MessageDigest.getInstance("SHA-256").digest(INPUT);
			case "lambda" -> {
				Supplier<String> supplier = () -> "order";
				yield supplier.get();
			}
			default -> throw new IllegalArgumentException(task);
		};
	}

}
