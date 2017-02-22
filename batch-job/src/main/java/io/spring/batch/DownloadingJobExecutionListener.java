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
package io.spring.batch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StreamUtils;

/**
 * @author Michael Minella
 */
public class DownloadingJobExecutionListener extends JobExecutionListenerSupport {

	@Autowired
	private ResourcePatternResolver resourcePatternResolver;

	@Value("${job.resource-path}")
	private String path;

	@Override
	public void beforeJob(JobExecution jobExecution) {

		try {
			Resource[] resources = this.resourcePatternResolver.getResources(this.path);

			StringBuilder paths = new StringBuilder();

			for (Resource resource : resources) {

				File file = File.createTempFile("input", ".csv");

				StreamUtils.copy(resource.getInputStream(), new FileOutputStream(file));

				paths.append(file.getAbsolutePath() + ",");
				System.out.println(">> downloaded file : " + file.getAbsolutePath());
			}

			jobExecution.getExecutionContext().put("localFiles", paths.substring(0, paths.length() - 1));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
