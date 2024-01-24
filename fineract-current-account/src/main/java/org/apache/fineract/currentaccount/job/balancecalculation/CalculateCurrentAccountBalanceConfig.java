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
package org.apache.fineract.currentaccount.job.balancecalculation;

import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountBalanceWriteService;
import org.apache.fineract.infrastructure.jobs.service.JobName;
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
public class CalculateCurrentAccountBalanceConfig {

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private CurrentAccountBalanceReadService currentAccountBalanceReadService;
    @Autowired
    private CurrentAccountBalanceWriteService currentAccountBalanceWriteService;

    @Bean
    protected Step calculateSnapshotBalanceStep() {
        return new StepBuilder(JobName.CALCULATE_CURRENT_ACCOUNT_BALANCE.name(), jobRepository)
                .tasklet(calculateCurrentAccountBalanceTasklet(), transactionManager).build();
    }

    @Bean
    public Job calculateCurrentAccountBalanceJob() {
        return new JobBuilder(JobName.CALCULATE_CURRENT_ACCOUNT_BALANCE.name(), jobRepository).start(calculateSnapshotBalanceStep())
                .incrementer(new RunIdIncrementer()).build();
    }

    @Bean
    public CalculateCurrentAccountBalanceTasklet calculateCurrentAccountBalanceTasklet() {
        return new CalculateCurrentAccountBalanceTasklet(currentAccountBalanceReadService, currentAccountBalanceWriteService);
    }
}
