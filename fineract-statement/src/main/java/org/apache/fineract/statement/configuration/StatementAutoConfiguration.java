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
package org.apache.fineract.statement.configuration;

import org.apache.fineract.statement.data.StatementParser;
import org.apache.fineract.statement.domain.ProductStatementRepository;
import org.apache.fineract.statement.provider.AccountStatementServiceProvider;
import org.apache.fineract.statement.service.ProductStatementService;
import org.apache.fineract.statement.service.ProductStatementServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan("org.apache.fineract.statement")
@ConditionalOnProperty("fineract.module.statement.enabled")
public class StatementAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ProductStatementService.class)
    public ProductStatementService productStatementService(StatementParser statementParser,
            ProductStatementRepository productStatementRepository, AccountStatementServiceProvider accountStatementServiceProvider) {
        return new ProductStatementServiceImpl(statementParser, productStatementRepository, accountStatementServiceProvider);
    }
}
