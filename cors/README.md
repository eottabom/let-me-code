# cors

CORS 를 헤더 이름으로 외우지 않고, 서버가 무엇을 돌려주고 브라우저가 무엇을 막는지 직접 확인하기 위한 예제입니다.

블로그: [CORS 제대로 이해하기: SOP부터 Preflight, 요청/응답 헤더, 에러 메시지까지](https://eottabom.github.io/post/cors-sop-preflight-headers/)

## 필요 환경

- JDK 25
- 로컬에 설치된 Chrome (브라우저 테스트가 `channel=chrome` 으로 실행합니다)
- 5173 포트가 비어 있어야 합니다 (브라우저 테스트가 프론트 페이지를 이 포트로 띄웁니다)

## 실행

### 자동화 테스트

```bash
./gradlew :cors:test
```

| 테스트 | 확인하는 것 |
|---|---|
| `CorsResponseHeaderIntegrationTests` | preflight 응답의 Allow-* 와 Vary, 허용 범위를 벗어난 요청을 Spring 이 CORS 헤더 없이 403 으로 거절하는지, Origin 이 없으면 CORS 검사를 하지 않는지, `allowedOrigins("*")` + `allowCredentials(true)` 가 요청 시점에 500 이 되는지, `allowedOriginPatterns("*")` 가 요청 출처를 그대로 돌려주는지 |
| `SecurityCorsIntegrationTests` | 인증을 요구하는 SecurityFilterChain 에서 `http.cors()` 가 없으면 preflight 가 401 이 되고, 켜면 `addCorsMappings` 설정으로 통과하는지 |
| `BrowserCorsIntegrationTests` | 실제 Chrome 에서 서버는 200 으로 처리했는데 응답이 막히는지, preflight 실패 시 본 요청이 가지 않는지, Simple Request 는 CORS 에러가 나도 컨트롤러가 실행되는지, Expose-Headers, 원인별 콘솔 메시지, Max-Age 캐시(기본 5초 포함), `Allow-Headers: *` 와 Authorization |

### 수동 확인 - 브라우저에서 직접 눌러보기

```bash
# API 서버 (8080)
./gradlew :cors:bootRun

# 프론트 페이지 (5173), 다른 터미널에서
python3 -m http.server 5173 -d cors/capture
```

http://localhost:5173 을 열고 DevTools 를 연 상태로 케이스 버튼을 누르면 Console, Network 탭에서 결과를 볼 수 있습니다.

### DevTools 캡처 (macOS)

블로그에 넣은 DevTools 캡처를 다시 찍는 스크립트입니다. 위 두 서버를 띄운 상태에서 실행합니다.

```bash
cd cors/capture
npm install
npx playwright install chromium
swiftc -O winid.swift -o winid

# 1번 케이스(설정 없는 GET)를 Console 탭으로 캡처
node devshot.js console 0 01-no-acao-console.png
# 여러 케이스를 한 화면에, 창 높이 지정
H=520 node devshot.js console 7,8,9 07-error-messages-console.png
```

- `page.screenshot()` 은 페이지 영역만 찍기 때문에, DevTools 를 연 브라우저 창을 `screencapture` 로 통째로 찍습니다
- 터미널에 macOS 화면 기록 권한이 필요합니다
- 창이 뜨자마자 원래 쓰던 앱으로 포커스를 돌려줘서, 캡처 중에 타이핑해도 주소창이나 DevTools 에 찍히지 않습니다 (osascript 사용)

## 볼 것

- `CorsConfig` - 경로마다 다른 CORS 정책 (`/api/none` 은 일부러 설정 없음)
- `FixedCorsHeaderFilter` - Nginx 처럼 고정된 CORS 헤더를 내려주는 경로. Spring 은 거절 시 헤더를 빼버려서 브라우저 메시지가 하나로 뭉치므로, 원인별 메시지를 보려면 이렇게 해야 합니다
- `RequestRecorder` - 서버에 도착한 요청과 컨트롤러까지 실행된 요청을 따로 기록
- `SecurityConfig` - `cors.require-authentication`, `cors.security-cors-enabled` 로 Security 와 CORS 관계를 바꿔가며 확인
