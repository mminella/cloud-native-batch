/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.spring.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import io.spring.batch.DownloadingStepExecutionListener;
import io.spring.batch.EnrichmentProcessor;
import io.spring.domain.Foo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.batch.partition.CommandLineArgsProvider;
import org.springframework.cloud.task.batch.partition.DeployerPartitionHandler;
import org.springframework.cloud.task.batch.partition.DeployerStepExecutionHandler;
import org.springframework.cloud.task.batch.partition.NoOpEnvironmentVariablesProvider;
import org.springframework.cloud.task.batch.partition.PassThroughCommandLineArgsProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.client.RestTemplate;

/**
 * @author Michael Minella
 */
@Configuration
public class JobConfiguration {

	@Profile("master")
	@Configuration
	public static class MasterConfiguration {

		@Bean
		public Step master(StepBuilderFactory stepBuilderFactory,
				Partitioner partitioner,
				PartitionHandler partitionHandler) {
			return stepBuilderFactory.get("master")
					.partitioner("load", partitioner)
					.partitionHandler(partitionHandler)
					.build();
		}

		@Bean
		public Job job(JobBuilderFactory jobBuilderFactory) throws Exception {
			return jobBuilderFactory.get("s3jdbc")
					.start(master(null, null, null))
					.build();
		}

		@Bean
		public Partitioner partitioner(ResourcePatternResolver resourcePatternResolver,
				@Value("${job.resource-path}") String resourcePath) throws IOException {
			Resource[] resources = resourcePatternResolver.getResources(resourcePath);

			MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
			partitioner.setResources(resources);

			return partitioner;
		}

		@Bean
		public PassThroughCommandLineArgsProvider commandLineArgsProvider() {

			List<String> commandLineArgs = new ArrayList<>(4);
			commandLineArgs.add("--spring.profiles.active=worker");
			commandLineArgs.add("--spring.cloud.task.initialize.enable=false");
			commandLineArgs.add("--spring.batch.initializer.enabled=false");
			commandLineArgs.add("--spring.datasource.initialize=false");

			PassThroughCommandLineArgsProvider provider = new PassThroughCommandLineArgsProvider(commandLineArgs);

			return provider;
		}

		@Bean
		public DeployerPartitionHandler partitionHandler(@Value("${job.worker-app}") String resourceLocation,
				@Value("${spring.application.name}") String applicationName,
				ApplicationContext context,
				TaskLauncher taskLauncher,
				JobExplorer jobExplorer,
				CommandLineArgsProvider commandLineArgsProvider) {
			DeployerPartitionHandler partitionHandler =
					new DeployerPartitionHandler(taskLauncher,
							jobExplorer,
							context.getResource(resourceLocation),
							"load");

			partitionHandler.setCommandLineArgsProvider(commandLineArgsProvider);
			partitionHandler.setEnvironmentVariablesProvider(new NoOpEnvironmentVariablesProvider());
			partitionHandler.setMaxWorkers(2);
			partitionHandler.setApplicationName(applicationName);

			return partitionHandler;
		}
	}

	@Profile("worker")
	@Configuration
	public static class WorkerConfiguration {

		@Bean
		public DeployerStepExecutionHandler stepExecutionHandler(ApplicationContext context, JobExplorer jobExplorer, JobRepository jobRepository) {
			return new DeployerStepExecutionHandler(context, jobExplorer, jobRepository);
		}

		@Bean
		public DownloadingStepExecutionListener downloadingStepExecutionListener() {
			return new DownloadingStepExecutionListener();
		}

		@Bean
		@StepScope
		public FlatFileItemReader<Foo> reader(@Value("#{stepExecutionContext['localFile']}")String fileName) throws Exception {
			FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>()
					.name("fooReader")
					.resource(new FileSystemResource(fileName))
					.delimited()
					.names(new String[] {"first", "second", "third"})
					.targetType(Foo.class)
					.build();

			return reader;
		}

		@Bean
		@StepScope
		public EnrichmentProcessor processor() {
			return new EnrichmentProcessor();
		}

		@Bean
		public JdbcBatchItemWriter<Foo> writer(DataSource dataSource) {

			return new JdbcBatchItemWriterBuilder<Foo>()
					.dataSource(dataSource)
					.beanMapped()
					.sql("INSERT INTO FOO VALUES (:first, :second, :third, :message)")
					.build();
		}

		@Bean
		public Step load(StepBuilderFactory stepBuilderFactory) throws Exception {
			return stepBuilderFactory.get("load")
					.<Foo, Foo>chunk(20)
					.reader(reader(null))
					.processor(processor())
					.writer(writer(null))
					.listener(downloadingStepExecutionListener())
					.build();
		}

		@Bean
		@LoadBalanced
		public RestTemplate restTemplate() {
			return new RestTemplate();
		}
	}
}
