"""실험 스크립트가 남긴 로그와 기록을 metric<TAB>value 줄이나 사람이 읽는 표로 바꾼다.

사용법: analyze.py <하위 명령> [인자...]
  stats                                   표준 입력의 metric<TAB>value 를 지표별 개수, 중앙값, 최솟값, 최댓값으로 묶는다
  requests <접두어> <앱 로그> <curl 시간 파일>   요청 순번 구간(1번째, 2~10번째, 11번째 이후)별 응답 시간
  jvm-windows <앱 로그> <클래스 로딩 로그> <컴파일 로그> [--detail]
                                          클래스 로딩, JIT 컴파일 수를 Ready 와 요청 순번 기준 구간으로 나눈다
  jfr-phases <jfr print --json 출력> <앱 로그> <부하 시작 epoch 초> [--timeline 파일]
                                          JFR 기록에서 구간별 CPU 사용량과 JIT 컴파일 수
  k6-summary <k6 요약 JSON>                 k6 결과를 부하 시작 시각(epoch 초) 한 줄과 구간별 응답 시간으로 바꾼다
"""
import json
import os
import re
import statistics
import sys
from collections import Counter, defaultdict
from datetime import datetime

READY_PATTERN = re.compile(r"^\[timeline\] \+ *(\d+)ms \S+ +ApplicationReadyEvent", re.M)


def stats(_args):
    """metric<TAB>value 줄을 받아 지표별 개수, 중앙값, 최솟값, 최댓값을 처음 나온 순서대로 출력한다."""
    values = {}
    for line in sys.stdin:
        parts = line.rstrip("\n").split("\t")
        if len(parts) != 2 or not parts[1]:
            continue
        values.setdefault(parts[0], []).append(float(parts[1]))

    width = max((len(name) for name in values), default=6)
    print(f"{'지표':<{width}}  {'n':>3}  {'중앙값':>8}  {'최소':>8}  {'최대':>8}")
    for name, numbers in values.items():
        print(f"{name:<{width}}  {len(numbers):>3}  {statistics.median(numbers):>8.2f}  {min(numbers):>8.2f}  {max(numbers):>8.2f}")


def requests(args):
    """[request] 로그와 curl 결과를 요청 순번 구간별로 묶어 metric<TAB>value 로 출력한다.

    curl 시간 파일은 한 줄에 한 요청씩 time_total(초)이다. 구간은 1번째, 2~10번째, 11번째 이후이고 구간 안은 중앙값을 쓴다.
    """
    prefix, app_log, curl_file = args[:3]
    server = [int(m.group(1)) / 1000 for m in re.finditer(r"^\[request\] .* server=(\d+)us$", open(app_log).read(), re.M)]
    client = [float(line) * 1000 for line in open(curl_file) if line.strip()]

    for name, numbers in (("서버", server), ("클라이언트", client)):
        for label, part in (("1번째", numbers[:1]), ("2~10번째", numbers[1:10]), ("11번째 이후", numbers[10:])):
            if part:
                print(f"{prefix} {name} {label}\t{statistics.median(part):.2f}")


def jvm_windows(args):
    """클래스 로딩, JIT 컴파일 로그를 Ready 와 요청 순번 기준 구간으로 나눠 센다.

    - 앱 로그: [timeline] 의 ApplicationReadyEvent 경과 시간, [request] 의 uptime(요청이 끝난 시점)과 server(처리 시간)를 쓴다.
    - 클래스 로딩 로그: -Xlog:class+load:file=...:uptimemillis 출력
    - 컴파일 로그: -Xlog:jit+compilation=debug:file=...:uptimemillis 출력 (-XX:+PrintCompilation 과 같은 내용)
    출력은 metric<TAB>value 이다. --detail 을 주면 1번째 요청 처리 중 로딩된 클래스의 패키지별 개수와
    같은 메서드가 C1 에서 C2 로 다시 컴파일된 예시를 사람이 읽는 형태로 출력한다.
    """
    app_log, class_log, compile_log = args[:3]
    detail = "--detail" in args
    app = open(app_log).read()
    ready = int(READY_PATTERN.search(app).group(1))
    hello = [(int(m.group(1)), int(m.group(2)) / 1000) for m in
             re.finditer(r"^\[request\] seq=\d+ uptime=(\d+)ms path=/hello status=\d+ server=(\d+)us$", app, re.M)]
    ends = [end for end, _ in hello]
    # uptime 은 요청이 끝난 시점이라, 처리 시간을 빼서 1번째 요청이 시작된 시점을 구한다 (ms 단위라 1ms 안팎 오차가 있다).
    first_start = hello[0][0] - hello[0][1]

    windows = [
        ("Ready 전", float("-inf"), ready),
        ("Ready 후 1번째 요청 전", ready, first_start),
        ("1번째 요청 처리 중", first_start, ends[0]),
        ("2~10번째 요청", ends[0], ends[9]),
        (f"11~{len(ends)}번째 요청", ends[9], ends[-1]),
    ]

    def window_of(ms):
        for name, start, end in windows:
            if start < ms <= end:
                return name
        return None

    def source_kind(source):
        if source.startswith("shared objects file"):
            return "CDS 아카이브"
        if source.startswith("jrt:/"):
            return "JDK 모듈"
        return "그 밖"

    classes = Counter()
    first_request_classes = []
    for line in open(class_log):
        m = re.match(r"\[(\d+)ms\] (\S+) source: (.*)$", line)
        if not m:
            continue
        window = window_of(int(m.group(1)))
        if window is None:
            continue
        classes[(window, source_kind(m.group(3)))] += 1
        if window == "1번째 요청 처리 중":
            first_request_classes.append(m.group(2))

    # [2069ms]  332  %  3       sun.invoke.util.Wrapper::forPrimitiveType (122 bytes)
    pattern = re.compile(r"^\[(\d+)ms\]\s+\d+\s+[%sbn! ]*?\s*([0-4])\s+(\S+)")
    compiles = Counter()
    levels_by_method = {}
    for line in open(compile_log):
        m = pattern.match(line)
        if not m or "made not entrant" in line:
            continue
        ms, level, method = int(m.group(1)), int(m.group(2)), m.group(3)
        levels_by_method.setdefault(method, []).append((ms, level))
        window = window_of(ms)
        if window is None:
            continue
        tier = "C1" if level in (1, 2, 3) else ("C2" if level == 4 else "기타")
        compiles[(window, tier)] += 1

    print(f"1번째 /hello 서버 시간 (ms)\t{hello[0][1]:.2f}")
    for name, _, _ in windows:
        for kind in ("CDS 아카이브", "JDK 모듈", "그 밖"):
            print(f"클래스 로딩 {name} {kind}\t{classes[(name, kind)]}")
        for tier in ("C1", "C2"):
            print(f"JIT 컴파일 {name} {tier}\t{compiles[(name, tier)]}")

    if not detail:
        return

    def group(name):
        if "$$Lambda" in name or "LambdaForm$" in name or "$$" in name:
            return "람다, 프록시 등 런타임 생성 클래스"
        parts = name.split(".")
        return ".".join(parts[:4]) if name.startswith("org.springframework") else ".".join(parts[:2])

    print(f"\n1번째 요청 처리 중 로딩된 클래스 {len(first_request_classes)}개, 묶음별 개수 (상위 12개)")
    for name, count in Counter(group(c) for c in first_request_classes).most_common(12):
        print(f"{count:5d}  {name}")

    print("\nReady 뒤 C1(3단계) 에서 C2(4단계) 로 다시 컴파일된 Spring, Jackson, Tomcat 메서드 예시")
    shown = 0
    for method, history in levels_by_method.items():
        if not method.startswith(("org.springframework", "tools.jackson", "org.apache")):
            continue
        in_requests = [(ms, level) for ms, level in history if ms > ready]
        if {3, 4} <= {level for _, level in in_requests}:
            print(method, " -> ".join(f"{level}단계@{ms}ms" for ms, level in in_requests))
            shown += 1
            if shown == 5:
                break


def jfr_phases(args):
    """JFR 기록에서 구간별 CPU 사용량과 JIT 컴파일 수를 낸다.

    - 구간: 시작 중(JVM 시작부터 Ready), Ready 후 대기, 요청 0~1초, 1~3초, 3~10초
    - 애플리케이션 CPU: 이 JVM 프로세스의 jdk.CPULoad 의 jvmUser + jvmSystem 평균에 코어 수를 곱해 '코어 몇 개를 썼는지'로 바꾼다
    - 컴파일러 스레드 CPU: jdk.ThreadCPULoad 중 C1, C2 CompilerThread 의 합을 같은 방식으로 바꾼다
    - 컴파일 수: jdk.Compilation 의 compiler(c1, c2)별 개수
    --timeline 을 주면 100ms 단위 프로세스 CPU 와 컴파일러 스레드 CPU 를 TSV 로 남긴다.
    """
    events = json.load(open(args[0]))["recording"]["events"]
    app = open(args[1]).read()
    load_epoch = float(args[2])
    timeline_file = args[args.index("--timeline") + 1] if "--timeline" in args else None
    cores = os.cpu_count()

    def epoch(text):
        # JFR 은 소수점 아래 9자리까지 쓰는데, fromisoformat 은 6자리까지만 받는다.
        return datetime.fromisoformat(re.sub(r"(\.\d{6})\d+", r"\1", text)).timestamp()

    jvm_start = epoch(next(e for e in events if e["type"] == "jdk.JVMInformation")["values"]["jvmStartTime"])
    ready = int(READY_PATTERN.search(app).group(1)) / 1000
    load = load_epoch - jvm_start
    phases = [("시작 중", 0, ready), ("Ready 후 대기", ready, load), ("요청 0~1초", load, load + 1),
              ("요청 1~3초", load + 1, load + 3), ("요청 3~10초", load + 3, load + 10)]

    def phase_of(seconds):
        for name, low, high in phases:
            if low <= seconds < high:
                return name
        return None

    process = defaultdict(list)
    thread_sum, compiler_sum = defaultdict(float), defaultdict(float)
    compiles = Counter()
    for e in events:
        values = e["values"]
        at = epoch(values["startTime"]) - jvm_start
        if e["type"] == "jdk.CPULoad":
            process[phase_of(at)].append(values["jvmUser"] + values["jvmSystem"])
        elif e["type"] == "jdk.ThreadCPULoad":
            bucket = round(at, 1)
            thread_sum[bucket] += values["user"] + values["system"]
            if "CompilerThread" in values["eventThread"]["javaName"]:
                compiler_sum[bucket] += values["user"] + values["system"]
        elif e["type"] == "jdk.Compilation":
            compiles[(phase_of(at), values["compiler"])] += 1

    compiler_by_phase = defaultdict(list)
    for bucket in thread_sum:
        compiler_by_phase[phase_of(bucket)].append(compiler_sum.get(bucket, 0.0))

    for name, _, _ in phases:
        if process[name]:
            print(f"{name} 애플리케이션 CPU (코어)\t{statistics.mean(process[name]) * cores:.2f}")
        if compiler_by_phase[name]:
            print(f"{name} 컴파일러 스레드 CPU (코어)\t{statistics.mean(compiler_by_phase[name]) * cores:.2f}")
        print(f"{name} C1 컴파일 수\t{compiles[(name, 'c1')]}")
        print(f"{name} C2 컴파일 수\t{compiles[(name, 'c2')]}")

    if timeline_file:
        with open(timeline_file, "w") as out:
            out.write("JVM 시작 기준 초\t애플리케이션 CPU (코어)\t컴파일러 스레드 CPU (코어)\t구간\n")
            for bucket in sorted(thread_sum):
                out.write(f"{bucket:.1f}\t{thread_sum[bucket] * cores:.2f}\t{compiler_sum.get(bucket, 0.0) * cores:.2f}\t{phase_of(bucket)}\n")


def k6_summary(args):
    """k6 결과를 부하 시작 시각(epoch 초) 한 줄과 metric<TAB>value 줄로 바꾼다."""
    summary = json.load(open(args[0]))
    print(f"{summary['start'] / 1000:.3f}")
    for p in summary["phases"]:
        print(f"{p['name']} 처리량 (초당 요청)\t{p['count'] / p['seconds']:.0f}")
        print(f"{p['name']} 평균 응답 시간 (ms)\t{p['avg']:.2f}")
        print(f"{p['name']} p99 응답 시간 (ms)\t{p['p99']:.2f}")
        print(f"{p['name']} 최대 응답 시간 (ms)\t{p['max']:.2f}")


COMMANDS = {
    "stats": stats,
    "requests": requests,
    "jvm-windows": jvm_windows,
    "jfr-phases": jfr_phases,
    "k6-summary": k6_summary,
}

if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] not in COMMANDS:
        sys.exit(__doc__)
    COMMANDS[sys.argv[1]](sys.argv[2:])
