package com.eottabom.letmecode.example.feignhttpclient;

import feign.Param;
import feign.RequestLine;

// Spring Cloud OpenFeign 의 {@code @FeignClient} 대신 feign-core 의 네이티브 애노테이션을 직접 사용한다. Spring Cloud 컨텍스트 없이도 {@link feign.Client} 구현체만 바꿔 끼우면 커넥션 처리 방식(HttpURLConnection / Apache HttpClient5 / OkHttp)을 비교할 수 있다.
public interface BackendClient {

	@RequestLine("GET /api/hello?delayMs={delayMs}")
	String hello(@Param("delayMs") long delayMs);

}
