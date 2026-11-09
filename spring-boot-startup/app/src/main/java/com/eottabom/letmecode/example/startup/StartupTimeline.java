package com.eottabom.letmecode.example.startup;

import java.lang.management.ManagementFactory;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;

// 시작 과정의 이벤트를 JVM 시작 기준 경과 시간과 함께 남긴다.
// 로깅 시스템이 준비되기 전 이벤트도 있어서 로거 대신 표준 출력에 [timeline] 접두어로 쓴다.
class StartupTimeline implements ApplicationListener<ApplicationEvent> {

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof AvailabilityChangeEvent<?> availability) {
			print("AvailabilityChangeEvent " + availability.getState().getClass().getSimpleName() + "="
					+ availability.getState());
		}
		else if (event instanceof SpringApplicationEvent || event instanceof ApplicationContextEvent
				|| event instanceof WebServerInitializedEvent) {
			print(event.getClass().getSimpleName());
		}
	}

	private static void print(String message) {
		long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
		System.out.printf("[timeline] +%6dms %-24s %s%n", uptimeMillis, Thread.currentThread().getName(), message);
	}

}
