package com.eottabom.letmecode.example.urlencoding;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

	/**
	 * 파이프('|') 로 구분된 복합 필터 조회.
	 *
	 * 예: GET /products?filter=category:전자기기|brand:삼성
	 *
	 * FE 에서 encodeURIComponent 없이 '|' 를 raw 로 전송하면 기본 Tomcat 이 400 을 반환한다. (The valid
	 * characters are defined in RFC 7230 and RFC 3986)
	 */
	@GetMapping("/products")
	public List<String> products(@RequestParam String filter) {
		return Arrays.asList(filter.split("\\|"));
	}

}
