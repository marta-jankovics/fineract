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
package org.apache.fineract.statement.provider;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.exception.PlatformServiceUnavailableException;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.statement.service.AccountStatementGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Scope("singleton")
public class AccountStatementGenerationServiceProvider {

    private static final String SERVICE_MISSING = "There is no AccountStatementGenerationService registered for this statement type: ";

    private final Map<PortfolioProductType, AccountStatementGenerationService> statementServices;

    @Autowired
    public AccountStatementGenerationServiceProvider(List<AccountStatementGenerationService> statementServices) {
        var mapBuilder = ImmutableMap.<PortfolioProductType, AccountStatementGenerationService>builder();
        for (AccountStatementGenerationService service : statementServices) {
            for (PortfolioProductType productType : PortfolioProductType.values()) {
                if (service.isSupportProductType(productType)) {
                    mapBuilder.put(productType, service);
                    log.info("Registered statement service '{}' for type/s '{}'", service, productType);
                }
            }
        }
        this.statementServices = mapBuilder.build();
    }

    public AccountStatementGenerationService findAccountStatementGenerationService(PortfolioProductType productType) {
        return statementServices.get(productType);
    }

    public AccountStatementGenerationService getAccountStatementGenerationService(PortfolioProductType productType) {
        AccountStatementGenerationService service = findAccountStatementGenerationService(productType);
        if (service == null) {
            throw new PlatformServiceUnavailableException("err.msg.statement.service.implementation.missing", SERVICE_MISSING + productType,
                    productType);
        }
        return service;
    }
}
