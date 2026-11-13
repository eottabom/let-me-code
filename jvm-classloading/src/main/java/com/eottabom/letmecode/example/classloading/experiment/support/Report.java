package com.eottabom.letmecode.example.classloading.experiment.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 실험 진행 로그와 결과를 화면과 결과 폴더의 {@code <실험>.txt} 에 함께 남긴다.
 */
public final class Report {

	private static final Logger log = LoggerFactory.getLogger(Report.class);

	private final Path textFile;

	private final String experiment;

	public Report(String experiment) {
		this.experiment = experiment;
		this.textFile = outputDir().resolve(experiment + ".txt");
		write(this.textFile, "", StandardOpenOption.TRUNCATE_EXISTING);
		print("JDK: " + ChildJvm.describe());
	}

	public void title(String title) {
		print("");
		print("== " + title);
	}

	public void print(String line) {
		log.info(line);
		write(this.textFile, line + "\n", StandardOpenOption.APPEND);
	}

	/**
	 * 대상 프로그램의 원본 출력을 {@code <실험>.<이름>.log} 로 남긴다. 개인 경로와 사용자 이름은 지운다.
	 * @param name 파일 이름에 넣을 경우 이름
	 * @param output 원본 출력
	 */
	public void saveRaw(String name, String output) {
		String home = System.getProperty("user.home");
		String user = System.getProperty("user.name");
		String sanitized = output.replace(home, "~").replace(user, "user");
		write(outputDir().resolve(this.experiment + "." + name + ".log"), sanitized,
				StandardOpenOption.TRUNCATE_EXISTING);
	}

	/**
	 * 실행 반복 횟수. 첫 프로세스는 따로 기록하고, 그 뒤로 이 횟수만큼 더 실행해 중앙값을 낸다.
	 * @return 반복 횟수
	 */
	public static int repeat() {
		return Integer.parseInt(System.getProperty("experiment.repeat", "10"));
	}

	private static Path outputDir() {
		Path dir = Path.of(System.getProperty("experiment.outputDir", "results/latest"));
		try {
			return Files.createDirectories(dir);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static void write(Path file, String text, StandardOpenOption option) {
		try {
			Files.writeString(file, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
					option);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
