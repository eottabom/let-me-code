## thread-dump

Java Thread Dump를 직접 수집하고 읽어보기 위한 예제 모듈입니다.
데드락, `BLOCKED` 상태, CPU 스핀 상태를 각각 재현합니다.

### 예제 목록

| 예제 | 실행 태스크 | 확인할 내용 |
| --- | --- | --- |
| `DeadlockExample` | `runDeadlockExample` | `Found one Java-level deadlock:` 블록 |
| `BlockedThreadExample` | `runBlockedThreadExample` | 같은 모니터 락을 기다리는 `BLOCKED` 스레드 |
| `CpuSpinExample` | `runCpuSpinExample` | `top -H`의 TID와 `jstack`의 `nid` 매핑 |

### 실행 방법

예제를 실행한 터미널은 그대로 두고, 다른 터미널에서 PID를 확인한 뒤 Thread Dump를 수집합니다.

```bash
./gradlew :thread-dump:runDeadlockExample
```

```bash
jps -l
jstack <pid>
```

CPU 스핀 예제는 OS 스레드 ID를 Thread Dump의 `nid`와 매핑합니다.

```bash
./gradlew :thread-dump:runCpuSpinExample
top -H -p <pid>
printf "%x\n" <tid>
jstack <pid> | grep -A 20 "nid=0x<hex>"
```

### 주의점

- `DeadlockExample`은 의도적으로 종료되지 않습니다. 확인 후 실행 프로세스를 종료해야 합니다.
- `BlockedThreadExample`은 락 보유 스레드가 60초 동안 모니터를 점유합니다.
- `CpuSpinExample`은 30초 동안 CPU를 점유하므로 로컬 환경에서만 실행합니다.
