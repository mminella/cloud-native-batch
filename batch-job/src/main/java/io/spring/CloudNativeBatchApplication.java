package io.spring;

import java.util.Properties;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.aws.autoconfigure.context.ContextStackAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.retry.annotation.EnableRetry;

@EnableTask
@EnableRetry
@EnableBatchProcessing
@SpringBootApplication(exclude = {ContextStackAutoConfiguration.class})
@EnableDiscoveryClient(autoRegister = false)
public class CloudNativeBatchApplication {

	public static void main(String[] args) {
		Properties properties = System.getProperties();
		properties.put("spring.profiles.active", "master");

		SpringApplication.run(CloudNativeBatchApplication.class, args);
	}
}
