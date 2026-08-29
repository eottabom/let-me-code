package com.eottabom.letmecode.example.feignhttpclient;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.LongFunction;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// concurrency 개 스레드로 동시 호출해 총 소요 시간을 잰다. 풀이 없으면 대기열 없이 새 소켓을 열기 쉬우므로,
// 차이는 elapsedMs 보다 커넥션 재사용 여부와 소켓 수에서 더 잘 드러난다.
final class LoadTestRunner {

	private static final Logger log = LoggerFactory.getLogger(LoadTestRunner.class);

	private LoadTestRunner() {
	}

	static void run(String label, int concurrency, long delayMs, LongFunction<String> call)
			throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(concurrency);

		List<Callable<String>> tasks = IntStream.range(0, concurrency)
			.<Callable<String>>mapToObj((i) -> () -> call.apply(delayMs))
			.toList();

		long start = System.nanoTime();
		List<Future<String>> futures;
		try {
			futures = executor.invokeAll(tasks);
		}
		finally {
			executor.shutdown();
		}
		long elapsedMs = (System.nanoTime() - start) / 1_000_000;

		int success = 0;
		int failure = 0;
		for (Future<String> future : futures) {
			try {
				future.get();
				success++;
			}
			catch (Exception ex) {
				failure++;
			}
		}

		log.info("[{}] concurrency={} delayMs={} elapsedMs={} success={} failure={}", label, concurrency, delayMs,
				elapsedMs, success, failure);
	}

	// 동시성 없이 순서대로 count 번 호출한다. 커넥션 재사용 여부(BackendController 의 remotePort 로그)를 보기 위한 용도.
	static void runSequential(String label, int count, long delayMs, LongFunction<String> call) {
		long start = System.nanoTime();
		for (int i = 0; i < count; i++) {
			call.apply(delayMs);
		}
		long elapsedMs = (System.nanoTime() - start) / 1_000_000;
		log.info("[{}] sequential count={} delayMs={} elapsedMs={}", label, count, delayMs, elapsedMs);
	}

	// concurrency 개 동시 버스트를 두 번 연달아 쏜다. default 는 idle 캐시(기본 5개)를 넘는 동시성에서
	// 두 번째 버스트도 새 remotePort 를 다시 연다. hc5/http-interface 는 maxTotal 만큼 재사용한다.
	static void runTwoRounds(String label, int concurrency, long delayMs, LongFunction<String> call)
			throws InterruptedException {
		log.info("[{}] --- round 1 ---", label);
		run(label, concurrency, delayMs, call);
		log.info("[{}] --- round 2 ---", label);
		run(label, concurrency, delayMs, call);
	}

}
