package com.eottabom.letmecode.example.cors;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

// 서버에 도착한 요청(arrived)과 컨트롤러까지 실행된 요청(handled)을 따로 남긴다.
// "브라우저에서는 CORS 에러인데 서버는 이미 실행했다" 를 테스트에서 확인하는 용도.
@Component
public class RequestRecorder {

	private static final Logger log = LoggerFactory.getLogger(RequestRecorder.class);

	private final List<String> arrived = new CopyOnWriteArrayList<>();

	private final List<String> handled = new CopyOnWriteArrayList<>();

	public void recordArrived(HttpServletRequest request, int status) {
		String line = describe(request);
		this.arrived.add(line);
		log.info("ARRIVED {} origin={} status={}", line, request.getHeader("Origin"), status);
	}

	public void recordHandled(HttpServletRequest request) {
		String line = describe(request);
		this.handled.add(line);
		log.info("HANDLER {} origin={}", line, request.getHeader("Origin"));
	}

	public List<String> arrived() {
		return List.copyOf(this.arrived);
	}

	public List<String> handled() {
		return List.copyOf(this.handled);
	}

	public void clear() {
		this.arrived.clear();
		this.handled.clear();
	}

	private String describe(HttpServletRequest request) {
		return request.getMethod() + " " + request.getRequestURI();
	}

}
