package com.eottabom.letmecode.example.classloading.target;

/**
 * 실험 1. 클래스가 실제로 언제 로딩되는지 관찰한다.
 * <ul>
 * <li>{@code Branch}: 인자가 {@code branch} 일 때만 실행하는 분기 안에서 쓴다.</li>
 * <li>{@code Holder}: {@code touch()} 만 부른다. {@code make()} 와 {@code unused()} 는 부르지 않는다.</li>
 * <li>{@code Base}, {@code Derived}: 호출하지 않는 {@code make()} 의 반환 타입과 반환 값으로만 나온다.</li>
 * <li>{@code Unused}: 호출하지 않는 {@code unused()} 의 반환 타입으로만 나온다.</li>
 * </ul>
 */
public final class LoadTiming {

	private LoadTiming() {
	}

	public static void main(String[] args) {
		Marker.mark("[mark] main 시작");
		if (args.length > 0 && "branch".equals(args[0])) {
			Marker.mark("[mark] 분기 진입");
			Branch.run();
		}
		Marker.mark("[mark] Holder.touch 호출 전");
		Holder.touch();
		Marker.mark("[mark] main 끝");
	}

	static final class Branch {

		private Branch() {
		}

		static void run() {
		}

	}

	static class Base {

	}

	static final class Derived extends Base {

	}

	static final class Unused {

	}

	static final class Holder {

		private Holder() {
		}

		static void touch() {
		}

		static Base make() {
			return new Derived();
		}

		static Unused unused() {
			return null;
		}

	}

}
