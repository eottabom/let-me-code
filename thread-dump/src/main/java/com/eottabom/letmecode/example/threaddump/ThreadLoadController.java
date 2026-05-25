package com.eottabom.letmecode.example.threaddump;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ThreadLoadController {

	private final Object lockA = new Object();

	private final Object lockB = new Object();

	private final Object monitor = new Object();

	private final AtomicBoolean isCpuSpinRunning = new AtomicBoolean(false);

	private final List<Thread> scenarioThreads = new ArrayList<>();

	@PostMapping("/load/deadlock")
	public String deadlock() {
		Thread threadA = new Thread(() -> {
			synchronized (this.lockA) {
				sleep(100);
				synchronized (this.lockB) {
					// 데드락 재현용 코드라 도달하지 않는다.
				}
			}
		}, "Actuator-Deadlock-A");

		Thread threadB = new Thread(() -> {
			synchronized (this.lockB) {
				sleep(100);
				synchronized (this.lockA) {
					// 데드락 재현용 코드라 도달하지 않는다.
				}
			}
		}, "Actuator-Deadlock-B");

		start(threadA);
		start(threadB);

		return "deadlock scenario started";
	}

	@PostMapping("/load/blocked")
	public String blocked() {
		Thread holder = new Thread(() -> {
			synchronized (this.monitor) {
				sleep(60_000);
			}
		}, "Actuator-Lock-Holder");

		start(holder);
		sleep(200);

		for (int i = 1; i <= 4; i++) {
			Thread waiter = new Thread(() -> {
				synchronized (this.monitor) {
					// Lock holder가 monitor를 놓은 뒤에만 진입한다.
				}
			}, "Actuator-Blocked-Thread-" + i);
			start(waiter);
		}

		return "blocked scenario started";
	}

	@PostMapping("/load/cpu-spin")
	public String cpuSpin() {
		if (!this.isCpuSpinRunning.compareAndSet(false, true)) {
			return "cpu spin scenario already running";
		}

		Thread spinner = new Thread(() -> {
			long count = 0;
			long endTime = System.currentTimeMillis() + 30_000;
			while (System.currentTimeMillis() < endTime) {
				count++;
			}
			this.isCpuSpinRunning.set(false);
			System.out.println("Actuator-CPU-Spinner stopped after " + count + " iterations");
		}, "Actuator-CPU-Spinner");

		start(spinner);

		return "cpu spin scenario started";
	}

	private void start(Thread thread) {
		this.scenarioThreads.add(thread);
		thread.start();
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

}
