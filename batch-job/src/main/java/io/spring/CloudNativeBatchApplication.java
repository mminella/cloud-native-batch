package io.spring;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.aws.autoconfigure.context.ContextStackAutoConfiguration;

@EnableBatchProcessing
@SpringBootApplication(exclude = {ContextStackAutoConfiguration.class})
public class CloudNativeBatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudNativeBatchApplication.class, args);
	}
}
