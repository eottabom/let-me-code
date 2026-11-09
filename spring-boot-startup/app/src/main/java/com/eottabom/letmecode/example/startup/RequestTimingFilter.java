package com.eottabom.letmecode.example.startup;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// 요청마다 순번과 서버 안에서 걸린 시간을 남긴다. 클라이언트 쪽 연결 비용과 나눠 보려는 것이다.
// 가장 바깥 필터로 두어 DispatcherServlet 초기화처럼 첫 요청에서 일어나는 일도 시간에 들어가게 한다.
// 실험 스크립트가 계속 부르는 /actuator 요청은 순번을 매기지 않는다.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestTimingFilter extends OncePerRequestFilter {

	private final AtomicLong sequence = new AtomicLong();

	private final StartupProperties properties;

	RequestTimingFilter(StartupProperties properties) {
		this.properties = properties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		long started = System.nanoTime();
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			long micros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - started);
			System.out.printf("[request] seq=%d uptime=%dms path=%s status=%d server=%dus%n",
					this.sequence.incrementAndGet(), ManagementFactory.getRuntimeMXBean().getUptime(),
					request.getRequestURI(), response.getStatus(), micros);
		}
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if (this.properties.logRequests()) {
			return request.getRequestURI().startsWith("/actuator");
		}
		return true;
	}

}
