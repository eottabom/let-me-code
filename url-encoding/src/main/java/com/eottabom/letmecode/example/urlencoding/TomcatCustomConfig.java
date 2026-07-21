package com.eottabom.letmecode.example.urlencoding;

import org.apache.coyote.http11.Http11NioProtocol;

import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("tomcat-relaxed")
public class TomcatCustomConfig {

	/**
	 * Tomcat 기본 설정은 RFC 7230 §3.2.6 / RFC 3986 §2.2 에서 정의한 allowed characters 만 허용한다.
	 * relaxedQueryChars 로 추가 허용할 문자를 지정하면 400 없이 통과한다. 주의: 보안 정책 검토 후 필요한 문자만 최소한으로 열어야
	 * 한다.
	 */
	@Bean
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatRelaxedQueryChars() {
		return (factory) -> factory.addConnectorCustomizers((connector) -> {
			if (connector.getProtocolHandler() instanceof Http11NioProtocol protocol) {
				protocol.setRelaxedQueryChars("|");
			}
		});
	}

}
