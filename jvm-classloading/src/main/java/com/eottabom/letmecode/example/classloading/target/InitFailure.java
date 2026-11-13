package com.eottabom.letmecode.example.classloading.target;

/**
 * 실험 4. static 초기화에서 {@link RuntimeException} 이 났을 때, 같은 클래스에 처음 접근할 때와 다시 접근할 때 어떤 예외가 나는지 본다.
 * <p>
 * {@link Error} 계열을 던지면 {@link ExceptionInInitializerError} 로 감싸지지 않고 그대로 전파되므로, 이 실험은 {@link RuntimeException}
 * 으로 한정한다.
 */
public final class InitFailure {

	private InitFailure() {
	}

	public static void main(String[] args) {
		access("1번째 접근");
		access("2번째 접근");
	}

	private static void access(String label) {
		try {
			System.out.println(Config.VALUE);
		}
		catch (Throwable ex) {
			System.out.println("[" + label + "] " + ex);
			for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
				System.out.println("  cause: " + cause);
			}
		}
	}

	static final class Config {

		static final String VALUE = load();

		private Config() {
		}

		private static String load() {
			throw new IllegalStateException("설정 파일을 찾을 수 없다");
		}

	}

}
