package com.eottabom.letmecode.example.feignhttpclient;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

// Feign 을 걷어내고 Spring 6 HTTP Interface 로 옮긴 형태. {@code @FeignClient} 대신 {@code @GetExchange} 를 쓰고, 실제 호출부는 {@link HttpInterfaceClientFactory} 에서 {@code HttpServiceProxyFactory} 로 생성한다.
public interface HttpInterfaceBackendClient {

	@GetExchange("/api/hello")
	String hello(@RequestParam long delayMs);

}
