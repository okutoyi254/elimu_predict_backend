package com.elimupredict;

import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ElimuPredictApplication {

	public static void main(String[] args) {
		SpringApplication.run(ElimuPredictApplication.class, args);
	}

	@Bean
	public ApplicationRunner verifyVirtualThreads() {
		return args -> {
			Thread.ofVirtual().start(() ->
					LoggerFactory.getLogger(ElimuPredictApplication.class)
							.info(" JVM Virtual threads active: {}",
									Thread.currentThread().isVirtual())
			);
		};
	}
}
