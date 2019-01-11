package com.example.twelvefactor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class TwelveFactorApplication {

	public static void main(String[] args) {
		SpringApplication.run(TwelveFactorApplication.class, args);
	}

}

