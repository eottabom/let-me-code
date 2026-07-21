package com.eottabom.letmecode.example.threaddump;

/**
 * Demonstrates BLOCKED thread state.
 *
 * Thread-1 holds the monitor on SharedResource and sleeps for a long time. Thread-2
 * through Thread-5 try to enter the same synchronized block and become BLOCKED waiting
 * for the monitor.
 *
 * Run this class, then capture a thread dump with: jstack <pid> You will see threads in
 * state BLOCKED (on object monitor).
 */
public class BlockedThreadExample {

	private static final Object monitor = new Object();

	public static void main(String[] args) throws InterruptedException {
		Thread holder = new Thread(() -> {
			synchronized (monitor) {
				System.out.println(Thread.currentThread().getName() + ": holding the monitor for 60 seconds...");
				sleep(60_000);
			}
		}, "Lock-Holder");

		holder.start();
		Thread.sleep(200);

		for (int i = 1; i <= 4; i++) {
			Thread waiter = new Thread(() -> {
				System.out.println(Thread.currentThread().getName() + ": waiting to acquire monitor...");
				synchronized (monitor) {
					System.out.println(Thread.currentThread().getName() + ": acquired monitor");
				}
			}, "Blocked-Thread-" + i);
			waiter.start();
		}

		holder.join();
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
