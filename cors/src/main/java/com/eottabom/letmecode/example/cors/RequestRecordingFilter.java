package com.eottabom.letmecode.example.cors;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Security 필터 체인보다 뒤에서 동작한다. Security 가 preflight 를 막으면 여기까지 오지 않는다.
@Component
public class RequestRecordingFilter extends OncePerRequestFilter {

	private final RequestRecorder requestRecorder;

	public RequestRecordingFilter(RequestRecorder requestRecorder) {
		this.requestRecorder = requestRecorder;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		filterChain.doFilter(request, response);
		this.requestRecorder.recordArrived(request, response.getStatus());
	}

}
