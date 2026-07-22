# url-encoding

파이프(`|`) 문자처럼 RFC 7230 / RFC 3986 에서 허용하지 않는 문자가 URL 쿼리스트링에 들어올 때  
Tomcat 이 어떻게 반응하는지, 그리고 어떤 방법으로 해결할 수 있는지 확인하는 예제입니다.

---

## 배경

HTTP/1.1 명세(RFC 7230 §3.2.6)와 URI 문법(RFC 3986 §2.2)은 URL 에서 사용 가능한 문자를 정의합니다.  
파이프(`|`, U+007C)는 두 명세 모두에서 *허용되지 않는* 문자로 분류됩니다.

FE 에서 `encodeURIComponent` 없이 파이프를 그대로 URL 에 담아 보내면  
Tomcat 은 파싱 단계에서 요청을 거절하며 아래 메시지를 로그에 남깁니다.

```
java.lang.IllegalArgumentException:
  Invalid character found in the request target [/products?filter=category:electronics|brand:samsung ].
  The valid characters are defined in RFC 7230 and RFC 3986
```

---

## 케이스 정리

| 케이스 | FE 인코딩 | Tomcat 설정 | 결과 |
|--------|-----------|-------------|------|
| 1 | 미인코딩 (`\|` 그대로) | 기본 | **400 Bad Request** |
| 2 | 인코딩 (`%7C`) | 기본 | **200 OK** |
| 3 | 미인코딩 (`\|` 그대로) | relaxed (`\|` 허용) | **200 OK** |
| 4 | 인코딩 (`%7C`) | relaxed (`\|` 허용) | **200 OK** |

---

## 해결 방법

### 방법 A — FE 에서 인코딩 (권장)

```js
const encoded = encodeURIComponent('category:electronics|brand:samsung');
fetch('/products?filter=' + encoded);
// → /products?filter=category%3Aelectronics%7Cbrand%3Asamsung
```

### 방법 B — Tomcat `relaxedQueryChars` 설정

```java
@Bean
public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatRelaxedQueryChars() {
    return (factory) -> factory.addConnectorCustomizers((connector) -> {
        if (connector.getProtocolHandler() instanceof Http11NioProtocol protocol) {
            protocol.setRelaxedQueryChars("|");
        }
    });
}
```

> **주의**: 보안 정책 검토 후 필요한 문자만 최소한으로 허용해야 합니다.

---

## 실행 방법

### 케이스 1·2 — 기본 Tomcat 프로필

```bash
# 백엔드 실행 (기본 프로필)
./gradlew :url-encoding:bootRun

# 프론트엔드 서버 실행
node url-encoding/front/server.js

# 브라우저에서 http://localhost:3000 접속
```

### 케이스 3·4 — relaxed Tomcat 프로필

```bash
# 백엔드 실행 (tomcat-relaxed 프로필)
./gradlew :url-encoding:bootRun --args='--spring.profiles.active=tomcat-relaxed'

# 프론트엔드 서버 실행
node url-encoding/front/server.js

# 브라우저에서 http://localhost:3000 접속
```

---

## 테스트 실행

```bash
# 전체 테스트 (unit + E2E)
./gradlew :url-encoding:test

# checkstyle + format
./gradlew :url-encoding:checkFormat :url-encoding:checkstyleMain :url-encoding:checkstyleTest
```

### 테스트 클래스

| 클래스 | 설명 |
|--------|------|
| `SearchControllerDefaultTests` | 기본 Tomcat: 단일 필터·인코딩 요청 검증 (케이스 2) |
| `SearchControllerRelaxedTests` | relaxed Tomcat: 인코딩 요청 검증 (케이스 3·4) |
| `SearchE2eTests` | Playwright: raw `\|` → 400, `%7C` → 200 (케이스 1·2) |
| `SearchE2eRelaxedTests` | Playwright: raw `\|` → 200, `%7C` → 200 (케이스 3·4) |
| `FrontE2eTests` | Playwright: front/index.html 버튼 클릭 → BE 응답 검증 |

