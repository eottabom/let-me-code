package com.eottabom.letmecode.example.threaddump;

/**
 * Demonstrates a CPU-spinning thread in RUNNABLE state.
 *
 * The spinning thread runs an infinite loop with no I/O or blocking calls, causing high
 * CPU usage. This is a common pattern seen during CPU spikes.
 *
 * Steps to identify the culprit thread: 1. top -H -p <pid> — find the thread ID (TID)
 * with high CPU 2. JDK 18+ prints nid in decimal, so search jstack <pid> | grep -A 20
 * "nid=<tid> " as is. On JDK 17 and earlier nid is hex, so convert with printf "%x\n"
 * <tid> and search "nid=0x<hex> ".
 */
public class CpuSpinExample {

	public static void main(String[] args) throws InterruptedException {
		Thread spinner = new Thread(() -> {
			System.out.println("CPU-Spinner: starting infinite loop (RUNNABLE)...");
			long count = 0;
			while (!Thread.currentThread().isInterrupted()) {
				count++;
			}
			System.out.println("CPU-Spinner: stopped after " + count + " iterations");
		}, "CPU-Spinner");

		Thread normal = new Thread(() -> {
			System.out.println("Normal-Thread: doing periodic work (TIMED_WAITING most of the time)...");
			while (!Thread.currentThread().isInterrupted()) {
				try {
					Thread.sleep(1_000);
					System.out.println("Normal-Thread: tick");
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}, "Normal-Thread");

		spinner.start();
		normal.start();

		Thread.sleep(30_000);
		spinner.interrupt();
		normal.interrupt();

		spinner.join();
		normal.join();
	}

}
