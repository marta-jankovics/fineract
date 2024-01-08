/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.statement.job;

import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.statement.provider.AccountStatementServiceProvider;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class PublishAccountStatementsConfig {

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private AccountStatementServiceProvider statementServiceProvider;

    @Bean
    public Job publishStatementsJob() {
        return new JobBuilder(JobName.PUBLISH_STATEMENTS.name(), jobRepository).start(publishStatementsStep())
                .incrementer(new RunIdIncrementer()).build();
    }

    @Bean
    protected Step publishStatementsStep() {
        return new StepBuilder(JobName.PUBLISH_STATEMENTS.name(), jobRepository).tasklet(publishStatementsTasklet(), transactionManager)
                .build();
    }

    @Bean
    public PublishAccountStatementsTasklet publishStatementsTasklet() {
        return new PublishAccountStatementsTasklet(statementServiceProvider);
    }
}
