package com.eottabom.letmecode.example.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.gateway")
public record GatewayDemoProperties(
	String aServerUri,
	String bServerUri,
	String sessionSvcUri
) {
}
