#!/usr/bin/env bash
# 리소스 조건별로 하나씩 Pod 를 띄우고 고정 RPS 부하를 준다. warmup 구간은 집계에서 뺀다.
source "$(dirname "$0")/lib.sh"
out="${1:-$DEMO_DIR/results/matrix}"
rate="${RATE:-300}" warmup_s="${WARMUP_SECONDS:-60}" measure_s="${MEASURE_SECONDS:-180}" repeat="${REPEAT:-3}"
mkdir -p "$out"
conditions=(
	"default-large|8|4Gi|"
	"default-small|1|1Gi|"
	"g1-cpu-small|1|4Gi|-XX:+UseG1GC"
	"g1-small|1|1Gi|-XX:+UseG1GC"
	"g1-small-apc2|1|1Gi|-XX:+UseG1GC -XX:ActiveProcessorCount=2"
)
printf 'rate=%s warmup=%ss measure=%ss repeat=%s image=%s\n' "$rate" "$warmup_s" "$measure_s" "$repeat" "$IMAGE" > "$out/settings.txt"
for run in $(seq 1 "$repeat"); do
	for condition in "${conditions[@]}"; do
		IFS='|' read -r name cpu memory opts <<< "$condition"
		if [[ -n "${CASES:-}" && " $CASES " != *" $name "* ]]; then continue; fi
		log "$name run $run (cpu=$cpu memory=$memory opts=${opts:-none})"
		clear_apps
		deploy_app "$name" "$cpu" "$memory" "$opts"
		pod="$(app_pod "$name")"
		if [[ ! -f "$out/$name-flags.txt" ]]; then
			# 같은 컨테이너(cgroup)에서 새 JVM 을 띄워 인식값과 최종 플래그를 남긴다.
			$KCTL exec "$pod" -- sh -c "java $opts -Xlog:os+container=trace -Xlog:gc+init -XX:+PrintFlagsFinal -version" > "$out/$name-flags.txt" 2>&1
		fi
		prefix="$out/$name-run-$run"
		start_load "load-$name" "$name" "$rate" "${warmup_s}s" "${measure_s}s"
		sleep "$warmup_s"
		{ echo "MEASURE_START $(date -u +%Y-%m-%dT%H:%M:%S)"; $KCTL exec "$pod" -- cat /sys/fs/cgroup/cpu.stat; } > "$prefix.cpustat.txt"
		wait_load "load-$name" > "$prefix.k6.json"
		{ echo "MEASURE_END $(date -u +%Y-%m-%dT%H:%M:%S)"; $KCTL exec "$pod" -- cat /sys/fs/cgroup/cpu.stat; } >> "$prefix.cpustat.txt"
		$KCTL logs "$pod" | grep -E '^\[[0-9T:.+-]+\]\[[0-9.]+s\]' | gzip > "$prefix.gc.log.gz"
		$KCTL delete job "load-$name" --ignore-not-found >/dev/null
	done
done
clear_apps
python3 "$DEMO_DIR/run/summarize.py" "$out"
