## jvm-classloading

Spring 없이 순수 Java 로, JVM 이 클래스를 언제 로딩하고 초기화하는지, 코드를 처음 실행할 때 비용이 얼마나 드는지 확인하는 예제입니다.
실험마다 JVM 을 새로 띄우고, `-Xlog:class+load,class+init` 로그와 프로그램이 찍은 표시 줄을 같은 표준 출력에 남겨 순서로 비교합니다.

### 실행 환경

| 항목 | 값 |
| --- | --- |
| JDK | Azul Zulu 25.0.2 (`JVM_JAVA_HOME` 으로 지정) |
| 로그 옵션 | `-Xlog:class+load=info,class+init=info:stdout:uptime,tags` |
| 반복 | 실험 3 의 시간만 첫 프로세스 1번 + 10번, 나머지는 1번 |

### 구성

```
jvm-classloading/
├── src/main/java/.../classloading/
│   ├── target/                      ← 새 JVM 으로 띄우는 실험 대상 프로그램 (JDK 클래스만 씀)
│   │   ├── Marker.java              ← [mark] 표시 줄
│   │   ├── LoadTiming.java          ← 실험 1
│   │   ├── LoadVsInit.java          ← 실험 2
│   │   ├── FirstRun.java            ← 실험 3
│   │   └── InitFailure.java         ← 실험 4
│   └── experiment/                  ← 대상 프로그램을 띄우고 로그를 정리하는 러너
└── results/
```

대상 프로그램은 로거와 문자열 연결을 쓰지 않습니다. 둘 다 그 자체로 클래스를 로딩해서 관찰하려는 로그에 섞이기 때문입니다.

### 실행

```bash
export JVM_JAVA_HOME=<Zulu 25 경로>

./gradlew :jvm-classloading:run --args='all'
./gradlew :jvm-classloading:run --args='first-run' -Prepeat=3
```

| 실험 | 확인하는 것 |
| --- | --- |
| `load-timing` | 분기 안에서만 쓰는 클래스가 언제 로딩되는지, 호출하지 않는 메서드에 나오는 클래스가 로딩되는지 |
| `load-vs-init` | `Class.forName(이름, false, 로더)`, constant variable, `static final Integer`, 배열 생성, 하위 클래스를 통한 static 필드, static 메서드 호출에서 로딩과 초기화 여부 |
| `first-run` | `String.format`, 정규식, `DateTimeFormatter`, `MessageDigest`, 람다의 1번째, 2번째, 901\~1,000번째 실행 시간과 1번째, 2번째 실행에서 로딩된 클래스 수. CDS 기본과 `-Xshare:off` 비교 |
| `init-failure` | static 초기화에서 `RuntimeException` 이 났을 때 1번째, 2번째 접근의 예외 |

### 결과

- `results/2026-11-10-zulu-25.0.2/` 에 블로그에 실은 실행의 결과가 있습니다.
- 실험을 다시 실행하면 출력이 `results/latest/` 에 남습니다. 이 폴더는 git 에 올라가지 않습니다.
- `<실험>.txt` 는 실행 출력과 요약, `<실험>.<경우>.log` 는 대상 프로그램의 원본 출력입니다. 개인 경로는 `~` 로 바꿨습니다.

### 주의

- JVMS 는 클래스를 언제 로딩할지 구현에 맡깁니다. 실험 1, 2 의 순서는 이 HotSpot 에서 관찰한 결과입니다.
- 실험 3 의 1번째 실행 시간에는 클래스 로딩 말고도 객체 생성, 내부 캐시 초기화가 들어 있고, 901\~1,000번째 시간에는 JIT 컴파일 효과도 들어 있습니다. 로딩된 클래스 수만으로 시간의 원인을 정할 수 없습니다.
- 실험 3 의 첫 프로세스도 빌드와 앞선 실험이 JDK 파일을 이미 읽은 뒤라 디스크 캐시가 빈 상태는 아닙니다.
- `-Xshare:off` 는 JDK 의 기본 CDS 아카이브를 읽지 않게 할 뿐 파일을 바꾸지 않습니다.
