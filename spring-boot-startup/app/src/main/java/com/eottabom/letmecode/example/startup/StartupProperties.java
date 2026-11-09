package com.eottabom.letmecode.example.startup;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("startup")
public record StartupProperties(long runnerDelaySeconds, String stubUrl, boolean logRequests) {
}
