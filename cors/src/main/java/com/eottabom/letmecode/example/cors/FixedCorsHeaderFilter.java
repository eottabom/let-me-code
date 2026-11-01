package com.eottabom.letmecode.example.cors;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Nginx 나 게이트웨이처럼 요청과 상관없이 정해진 CORS 헤더를 그대로 내려주는 경로.
// Spring 은 허용 범위를 벗어난 요청을 헤더 없이 403 으로 거절해서 브라우저 메시지가 하나로 뭉치므로,
// 메서드, 헤더, credentials 별 브라우저 메시지를 보려면 이렇게 고정 헤더를 내려줘야 한다.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FixedCorsHeaderFilter extends OncePerRequestFilter {

	private static final String PREFIX = "/fixed/";

	private static final Map<String, String> MAX_AGE_BY_PATH = Map.of("/fixed/age0", "0", "/fixed/age600", "600");

	private final RequestRecorder requestRecorder;

	public FixedCorsHeaderFilter(RequestRecorder requestRecorder) {
		this.requestRecorder = requestRecorder;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith(PREFIX);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String path = request.getRequestURI();
		response.setHeader("Access-Control-Allow-Origin", CorsConfig.FRONT_ORIGIN);
		if (path.startsWith("/fixed/star")) {
			response.setHeader("Access-Control-Allow-Methods", "*");
			response.setHeader("Access-Control-Allow-Headers", "*");
			if (path.startsWith("/fixed/star-credentials")) {
				response.setHeader("Access-Control-Allow-Credentials", "true");
			}
		}
		else {
			response.setHeader("Access-Control-Allow-Methods", "GET, POST");
			response.setHeader("Access-Control-Allow-Headers", "Content-Type");
		}
		String maxAge = MAX_AGE_BY_PATH.get(path);
		if (maxAge != null) {
			response.setHeader("Access-Control-Max-Age", maxAge);
		}

		if ("OPTIONS".equals(request.getMethod())) {
			response.setStatus(HttpStatus.NO_CONTENT.value());
		}
		else {
			response.setContentType(MediaType.TEXT_PLAIN_VALUE);
			response.getWriter().write("ok");
		}
		this.requestRecorder.recordArrived(request, response.getStatus());
	}

}
