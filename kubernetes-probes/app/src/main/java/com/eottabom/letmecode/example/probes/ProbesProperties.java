package com.eottabom.letmecode.example.probes;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("probes")
public record ProbesProperties(long startupDelaySeconds, long runnerDelaySeconds, String dependencyHost, int dependencyPort) {
}
