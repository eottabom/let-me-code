package com.eottabom.letmecode.example.cors;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("cors")
public record CorsProperties(boolean requireAuthentication, boolean securityCorsEnabled) {
}
