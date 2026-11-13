package com.eottabom.letmecode.example.classloading.target;

/**
 * 실험 2. 클래스를 참조하는 방식에 따라 로딩만 되는지, 초기화까지 되는지 본다. 첫 인자로 경우를 고르고, 경우마다 JVM 을 새로 띄운다.
 * <p>
 * 각 클래스의 static 블록은 {@code [static]} 줄을 찍는다. 이 줄이 찍히면 그 클래스가 초기화된 것이다.
 */
public final class LoadVsInit {

	private static final String LAZY_CLASS_NAME = "com.eottabom.letmecode.example.classloading.target.LoadVsInit$Lazy";

	private LoadVsInit() {
	}

	public static void main(String[] args) throws ClassNotFoundException {
		Marker.mark("[mark] 실행 전");
		switch (args[0]) {
			// 로딩만 하고 초기화하지 않도록 initialize 를 false 로 넘긴다. 문자열 연결도 클래스를 로딩하므로 이름은 상수로 쓴다.
			case "forName-no-init" -> Class.forName(LAZY_CLASS_NAME, false, LoadVsInit.class.getClassLoader());
			// static final 이고 상수식으로 초기화한 기본형이라 JLS 의 constant variable 이다. 값이 호출하는 쪽에 그대로 들어간다.
			case "constant-variable" -> System.out.println(Constants.LIMIT);
			// static final 이지만 Integer 라서 constant variable 이 아니다. 필드를 읽으려면 클래스를 초기화해야 한다.
			case "static-final-boxed" -> System.out.println(Boxed.LIMIT);
			case "array" -> System.out.println(new Element[1].length);
			// Parent 에 선언된 static 필드를 Child 를 통해 읽는다.
			case "subclass-static-field" -> System.out.println(Child.shared);
			case "static-method" -> Util.call();
			default -> throw new IllegalArgumentException(args[0]);
		}
		Marker.mark("[mark] 실행 후");
	}

	static final class Lazy {

		static {
			System.out.println("[static] Lazy");
		}

		private Lazy() {
		}

	}

	static final class Constants {

		static final int LIMIT = 10;

		static {
			System.out.println("[static] Constants");
		}

		private Constants() {
		}

	}

	static final class Boxed {

		static final Integer LIMIT = 10;

		static {
			System.out.println("[static] Boxed");
		}

		private Boxed() {
		}

	}

	static final class Element {

		static {
			System.out.println("[static] Element");
		}

		private Element() {
		}

	}

	static class Parent {

		static int shared = 1;

		static {
			System.out.println("[static] Parent");
		}

	}

	static final class Child extends Parent {

		static {
			System.out.println("[static] Child");
		}

		private Child() {
		}

	}

	static final class Util {

		static {
			System.out.println("[static] Util");
		}

		private Util() {
		}

		static void call() {
		}

	}

}
