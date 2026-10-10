#!/usr/bin/env bash
# limits 와 JDK 버전에 따라 기본 GC 선택이 어떻게 달라지는지 확인한다. 각 Pod 는 java 명령만 실행하고 종료한다.
#   selection-boundary.txt : limits 경계별 기본 GC (앱 이미지, JDK 21)
#   jdk-versions.txt       : Temurin 8, 11, 17, 21 이미지별 기본 GC
#   server-option.txt      : limits cpu 1, mem 1Gi 에서 -server, -client, -XX:+AlwaysActAsServerClassMachine 별 GC
source "$(dirname "$0")/lib.sh"
out_dir="${1:-$DEMO_DIR/results}"
mkdir -p "$out_dir"

FLAGS_GREP="grep -E ' (UseSerialGC|UseParallelGC|UseG1GC|MaxHeapSize|ParallelGCThreads|ConcGCThreads|AlwaysActAsServerClassMachine) '"

# run_pod <name> <image> <cpu> <memory> <script> : Pod 를 띄워 script 를 실행하고 로그를 출력한다.
run_pod() {
	local name="$1" image="$2" cpu="$3" memory="$4" script="$5"
	$KCTL delete pod "$name" --ignore-not-found >/dev/null
	cat <<YAML | $KCTL apply -f - >/dev/null
apiVersion: v1
kind: Pod
metadata: { name: $name, namespace: jvm-gc, labels: { role: probe } }
spec:
  restartPolicy: Never
  containers:
    - name: java
      image: $image
      imagePullPolicy: IfNotPresent
      command: ["sh", "-c"]
      args:
        - |
$(printf '%s\n' "$script" | sed 's/^/          /')
      resources:
        requests: { cpu: "500m", memory: "$memory" }
        limits: { cpu: "$cpu", memory: "$memory" }
YAML
	$KCTL wait --for=jsonpath='{.status.phase}'=Succeeded "pod/$name" --timeout=300s >/dev/null
	$KCTL logs "$name"
	$KCTL delete pod "$name" --wait=false >/dev/null
}

selection_boundary() {
	local out="$out_dir/selection-boundary.txt" cpu memory opts
	: > "$out"
	for limits in "1 4Gi" "2 4Gi" "1 1791Mi -XX:ActiveProcessorCount=2" "1 1792Mi -XX:ActiveProcessorCount=2" "8 1Gi" "1 1Gi"; do
		read -r cpu memory opts <<<"$limits"
		{
			printf '\n## limits cpu=%s memory=%s options=%s\n' "$cpu" "$memory" "${opts:-none}"
			run_pod probe-boundary "$IMAGE" "$cpu" "$memory" "java $opts -Xlog:gc -Xlog:gc+init -version" |
				grep -E 'Using (Serial|G1)|CPUs:|Memory:|Heap Max'
		} | tee -a "$out"
	done
}

jdk_versions() {
	local out="$out_dir/jdk-versions.txt" jdk cpu memory
	: > "$out"
	for jdk in 8 11 17 21; do
		for limits in "1 1Gi" "1 4Gi" "2 1Gi" "2 1792Mi" "2 4Gi"; do
			read -r cpu memory <<<"$limits"
			{
				echo "## JDK $jdk limits cpu $cpu, mem $memory"
				run_pod "probe-jdk$jdk" "eclipse-temurin:$jdk-jre" "$cpu" "$memory" \
					"java -version 2>&1 | sed -n 2p; java -XX:+PrintFlagsFinal -version 2>/dev/null | $FLAGS_GREP"
			} | tee -a "$out"
		done
	done
}

server_option() {
	local out="$out_dir/server-option.txt" jdk
	local script="echo '# java -client -version'; java -client -version 2>&1 | tail -1
for o in '' -server -client -XX:+AlwaysActAsServerClassMachine; do
  echo \"# options: \$o\"; java \$o -XX:+PrintFlagsFinal -version 2>/dev/null | $FLAGS_GREP
done"
	: > "$out"
	for jdk in 8 11 17 21; do
		{
			echo "## JDK $jdk limits cpu 1, mem 1Gi"
			run_pod "probe-server$jdk" "eclipse-temurin:$jdk-jre" 1 1Gi "$script"
		} | tee -a "$out"
	done
}

selection_boundary
jdk_versions
server_option
