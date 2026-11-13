package com.eottabom.letmecode.example.classloading.experiment.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 실험 대상 프로그램을 JVM 을 새로 띄워 실행한다. 실험마다 새 JVM 을 써야 이전 실행이 로딩한 클래스가 결과에 섞이지 않는다.
 */
public final class ChildJvm {

	private ChildJvm() {
	}

	/**
	 * 대상 프로그램을 실행하고 표준 출력과 표준 에러를 합친 출력을 돌려준다.
	 * @param jvmOptions 넘길 JVM 옵션
	 * @param mainClass 대상 프로그램의 main 클래스
	 * @param args 프로그램 인자
	 * @return 출력
	 */
	public static String run(List<String> jvmOptions, Class<?> mainClass, String... args) {
		List<String> command = new ArrayList<>();
		command.add(java());
		command.addAll(jvmOptions);
		// 로그의 source 경로를 짧게 하려고 이 모듈의 클래스 폴더만 클래스패스로 넘긴다.
		command.add("-cp");
		command.add(classesDir(mainClass));
		command.add(mainClass.getName());
		command.addAll(List.of(args));
		try {
			Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
			String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				throw new IllegalStateException(String.join(" ", command) + " 가 " + exitCode + " 로 끝났다\n" + output);
			}
			return output;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * 대상 프로그램을 띄우는 JDK 의 {@code java -version} 출력과 CPU 수.
	 * @return 설명
	 */
	public static String describe() {
		String version = run(List.of("-version"), ChildJvm.class).lines().skip(1).findFirst().orElse("");
		return version + ", " + System.getProperty("os.arch") + ", CPU " + Runtime.getRuntime().availableProcessors() + "개";
	}

	private static String java() {
		String javaHome = System.getProperty("jvm.javaHome", "");
		if (javaHome.isBlank()) {
			javaHome = System.getProperty("java.home");
		}
		return Path.of(javaHome, "bin", "java").toString();
	}

	private static String classesDir(Class<?> mainClass) {
		try {
			return Path.of(mainClass.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
