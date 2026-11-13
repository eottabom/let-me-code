package com.eottabom.letmecode.example.classloading.target;

/**
 * 실험 대상 프로그램이 지금 어디를 실행하는지 표준 출력에 남긴다.
 * <p>
 * {@code -Xlog:class+load,class+init:stdout} 로그와 같은 표준 출력에 찍어서, 로그 줄과 표시 줄의 순서로 로딩 시점을 본다. 로거나 문자열 연결을
 * 쓰면 그 자체로 클래스가 로딩되어 관찰하려는 로그에 섞이므로, {@code System.out} 에 문자열 상수만 넘긴다.
 */
final class Marker {

	private Marker() {
	}

	/**
	 * 표시 줄을 한 번에 찍는다. 두 번에 나눠 찍으면 그 사이에 JVM 로그 줄이 끼어들 수 있어서, 부르는 쪽이 {@code [mark]} 를 붙인 상수를 넘긴다.
	 * @param line {@code [mark]} 로 시작하는 줄
	 */
	static void mark(String line) {
		System.out.println(line);
	}

}
