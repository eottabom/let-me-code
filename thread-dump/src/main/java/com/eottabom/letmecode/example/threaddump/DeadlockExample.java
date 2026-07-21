package com.eottabom.letmecode.example.threaddump;

/**
 * Demonstrates a classic deadlock between two threads.
 *
 * Thread-A acquires lockA then waits for lockB. Thread-B acquires lockB then waits for
 * lockA.
 *
 * Run this class, then capture a thread dump with: jstack <pid> Look for "Found one
 * Java-level deadlock:" at the bottom of the output.
 */
public class DeadlockExample {

	private static final Object lockA = new Object();

	private static final Object lockB = new Object();

	public static void main(String[] args) {
		Thread threadA = new Thread(() -> {
			synchronized (lockA) {
				System.out.println("Thread-A: acquired lockA, waiting for lockB...");
				sleep(100);
				synchronized (lockB) {
					System.out.println("Thread-A: acquired lockB");
				}
			}
		}, "Thread-A");

		Thread threadB = new Thread(() -> {
			synchronized (lockB) {
				System.out.println("Thread-B: acquired lockB, waiting for lockA...");
				sleep(100);
				synchronized (lockA) {
					System.out.println("Thread-B: acquired lockA");
				}
			}
		}, "Thread-B");

		threadA.start();
		threadB.start();

		try {
			threadA.join();
			threadB.join();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
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
