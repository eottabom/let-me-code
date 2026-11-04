package com.eottabom.letmecode.example.probes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ProbesApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProbesApplication.class, args);
	}

}
