package com.eottabom.letmecode.example.jvmgc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pod 리소스에 따라 JVM 기본 GC, GC worker 수, Heap 크기가 바뀌는지 확인하기 위한 게시글 피드 API.
 */
@SpringBootApplication
public class JvmGcDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(JvmGcDemoApplication.class, args);
	}

}
