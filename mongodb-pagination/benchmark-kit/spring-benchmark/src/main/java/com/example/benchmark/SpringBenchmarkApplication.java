package com.example.benchmark;

import com.example.benchmark.config.BenchmarkProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@EnableConfigurationProperties(BenchmarkProperties.class)
public class SpringBenchmarkApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SpringBenchmarkApplication.class, args);
        int exitCode = SpringApplication.exit(context);
        System.exit(exitCode);
    }
}
