package com.eottabom.letmecode.example.benchmark.finalkeyword;

public class MethodTest {

	private final int[] numbers = new int[1000];

	public MethodTest() {
		for (int i = 0; i < this.numbers.length; i++) {
			this.numbers[i] = i;
		}
	}

	public int nonFinalMethod() {
		int sum = 0;
		for (int number : this.numbers) {
			sum += complexCalculation(number);
		}
		return sum;
	}

	public final int finalMethod() {
		int sum = 0;
		for (int number : this.numbers) {
			sum += complexCalculationFinal(number);
		}
		return sum;
	}

	private int complexCalculation(int value) {
		return (value * 31) ^ (value >>> 2);
	}

	private final int complexCalculationFinal(int value) {
		return (value * 31) ^ (value >>> 2);
	}

}
