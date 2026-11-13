package com.eottabom.letmecode.example.classloading.experiment;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 대상 프로그램의 표준 출력에 섞인 {@code -Xlog:class+load,class+init} 로그와 {@code [mark]} 줄을 읽는다.
 */
final class ClassLog {

	/**
	 * 로딩, 초기화 로그를 남길 때 쓰는 JVM 옵션. 표시 줄과 순서를 맞추려고 같은 표준 출력에 남긴다.
	 */
	static final String LOG_OPTION = "-Xlog:class+load=info,class+init=info:stdout:uptime,tags";

	private static final Pattern LOADED = Pattern.compile("\\[class,load\\s*\\] (\\S+) source: (.*)");

	private static final Pattern INITIALIZED = Pattern.compile("Initializing '([^']+)'");

	private final List<String> lines;

	ClassLog(String output) {
		this.lines = output.lines().toList();
	}

	/**
	 * 두 표시 줄 사이의 줄. 표시가 없으면 처음부터, 또는 끝까지로 본다.
	 * @param from 시작 표시 (포함하지 않음), null 이면 처음부터
	 * @param to 끝 표시 (포함하지 않음), null 이면 끝까지
	 * @return 구간의 로그
	 */
	ClassLog between(String from, String to) {
		int start = (from != null) ? this.lines.indexOf("[mark] " + from) + 1 : 0;
		int end = (to != null) ? this.lines.indexOf("[mark] " + to) : this.lines.size();
		return new ClassLog(String.join("\n", this.lines.subList(start, end)));
	}

	/**
	 * 로딩된 클래스 이름. 이름에 {@code filter} 가 들어간 것만 고른다.
	 * @param filter 클래스 이름에 들어 있어야 하는 문자열
	 * @return 로딩 순서대로 클래스 이름
	 */
	Set<String> loaded(String filter) {
		Set<String> names = new LinkedHashSet<>();
		for (String line : this.lines) {
			Matcher matcher = LOADED.matcher(line);
			if (matcher.find() && matcher.group(1).contains(filter)) {
				names.add(matcher.group(1));
			}
		}
		return names;
	}

	/**
	 * 초기화된 클래스 이름. 로그의 {@code a/b/C} 형식을 {@code a.b.C} 로 바꾼다.
	 * @param filter 클래스 이름에 들어 있어야 하는 문자열
	 * @return 초기화 순서대로 클래스 이름
	 */
	Set<String> initialized(String filter) {
		Set<String> names = new LinkedHashSet<>();
		for (String line : this.lines) {
			Matcher matcher = INITIALIZED.matcher(line);
			if (matcher.find()) {
				String name = matcher.group(1).replace('/', '.');
				if (name.contains(filter)) {
					names.add(name);
				}
			}
		}
		return names;
	}

	/**
	 * 로딩된 클래스 수와 그 가운데 CDS 아카이브({@code shared objects file})에서 읽은 수.
	 * @return 전체 수, CDS 에서 읽은 수
	 */
	int[] loadedCount() {
		int total = 0;
		int shared = 0;
		for (String line : this.lines) {
			Matcher matcher = LOADED.matcher(line);
			if (matcher.find()) {
				total++;
				if (matcher.group(2).startsWith("shared objects file")) {
					shared++;
				}
			}
		}
		return new int[] { total, shared };
	}

	/**
	 * 클래스 이름에서 {@code $} 뒤의 중첩 클래스 이름만 남긴다.
	 * @param names 클래스 이름
	 * @return 쉼표로 이은 이름, 없으면 {@code -}
	 */
	static String simpleNames(Set<String> names) {
		if (names.isEmpty()) {
			return "-";
		}
		return names.stream().map((name) -> name.substring(name.indexOf('$') + 1)).collect(Collectors.joining(", "));
	}

	/**
	 * 이름에 {@code filter} 가 들어간 로딩, 초기화, 검증 로그와 표시 줄, static 블록 출력만 남긴다.
	 * @param filter 클래스 이름에 들어 있어야 하는 문자열
	 * @return 고른 줄
	 */
	List<String> trace(String filter) {
		List<String> picked = new ArrayList<>();
		for (String line : this.lines) {
			if (line.startsWith("[mark]") || line.startsWith("[static]") || line.contains(filter)) {
				picked.add(line.replaceAll("source: file:\\S+", "source: 클래스 폴더"));
			}
		}
		return picked;
	}

}
