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

import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.exception.PlatformServiceUnavailableException;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.statement.domain.StatementPublishType;
import org.apache.fineract.statement.domain.StatementType;
import org.apache.fineract.statement.service.AccountStatementGenerationReadService;
import org.apache.fineract.statement.service.AccountStatementGenerationWriteService;
import org.apache.fineract.statement.service.AccountStatementPublishReadService;
import org.apache.fineract.statement.service.AccountStatementPublisher;
import org.apache.fineract.statement.service.AccountStatementWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Scope("singleton")
public class AccountStatementServiceProvider {

    private static final String SERVICE_MISSING = "There is no Statement Service registered for this statement type: ";

    private final Map<PortfolioProductType, AccountStatementGenerationReadService> generationReadServicesMap;
    private final Map<String, AccountStatementGenerationWriteService> generationWriteServicesMap;
    private final Map<PortfolioProductType, AccountStatementPublishReadService> publishReadServicesMap;
    private final Map<String, AccountStatementPublisher> publishWriteServicesMap;
    private final Map<PortfolioProductType, AccountStatementWriteService> statementWriteServicesMap;

    @Autowired
    public AccountStatementServiceProvider(List<AccountStatementGenerationReadService> generationReadServices,
            List<AccountStatementGenerationWriteService> generationWriteServices,
            List<AccountStatementPublishReadService> publishReadServices, List<AccountStatementPublisher> publishWriteServices,
            List<AccountStatementWriteService> statementWriteServices) {
        HashMap<PortfolioProductType, AccountStatementGenerationReadService> generationReadMap = new HashMap<>();
        Arrays.stream(PortfolioProductType.values())
                .forEach(pt -> generationReadServices.stream().filter(e -> e.isSupport(pt)).findFirst().ifPresent(e -> {
                    generationReadMap.put(pt, e);
                    log.info("Registered statement read service '{}' for type/s '{}'", e, pt);
                }));
        this.generationReadServicesMap = generationReadMap;
        HashMap<String, AccountStatementGenerationWriteService> generationWriteMap = new HashMap<>();
        Arrays.stream(PortfolioProductType.values())
                .forEach(pt -> Arrays.stream(StatementType.values()).forEach(st -> Arrays.stream(StatementPublishType.values())
                        .forEach(pu -> generationWriteServices.stream().filter(e -> e.isSupport(pt, st, pu)).findFirst().ifPresent(e -> {
                            generationWriteMap.put(buildTypeKey(pt, st, pu), e);
                            log.info("Registered statement write service '{}' for type/s '{}-{}-{}'", e, pt, st, pu);
                        }))));
        this.generationWriteServicesMap = generationWriteMap;
        HashMap<PortfolioProductType, AccountStatementPublishReadService> publishReadMap = new HashMap<>();
        Arrays.stream(PortfolioProductType.values())
                .forEach(pt -> publishReadServices.stream().filter(e -> e.isSupport(pt)).findFirst().ifPresent(e -> {
                    publishReadMap.put(pt, e);
                    log.info("Registered statement read service '{}' for type/s '{}'", e, pt);
                }));
        this.publishReadServicesMap = publishReadMap;
        HashMap<String, AccountStatementPublisher> publishWriteMap = new HashMap<>();
        Arrays.stream(PortfolioProductType.values())
                .forEach(pt -> Arrays.stream(StatementType.values()).forEach(st -> Arrays.stream(StatementPublishType.values())
                        .forEach(pu -> publishWriteServices.stream().filter(e -> e.isSupport(pt, st, pu)).findFirst().ifPresent(e -> {
                            publishWriteMap.put(buildTypeKey(pt, st, pu), e);
                            log.info("Registered statement publish service '{}' for type/s '{}-{}-{}'", e, pt, st, pu);
                        }))));
        this.publishWriteServicesMap = publishWriteMap;
        HashMap<PortfolioProductType, AccountStatementWriteService> statementWriteMap = new HashMap<>();
        Arrays.stream(PortfolioProductType.values())
                .forEach(pt -> statementWriteServices.stream().filter(e -> e.isSupport(pt)).findFirst().ifPresent(e -> {
                    statementWriteMap.put(pt, e);
                    log.info("Registered statement service '{}' for type/s '{}'", e, pt);
                }));
        this.statementWriteServicesMap = statementWriteMap;
    }

    public AccountStatementGenerationReadService findAccountStatementGenerationReadService(@NotNull PortfolioProductType productType) {
        return generationReadServicesMap.get(productType);
    }

    @NotNull
    public AccountStatementGenerationReadService getAccountStatementGenerationReadService(@NotNull PortfolioProductType productType) {
        AccountStatementGenerationReadService service = findAccountStatementGenerationReadService(productType);
        if (service == null) {
            throw new PlatformServiceUnavailableException("err.msg.statement.generation.read.service.implementation.missing",
                    SERVICE_MISSING + productType, productType);
        }
        return service;
    }

    public AccountStatementGenerationWriteService findAccountStatementGenerationWriteService(@NotNull PortfolioProductType productType,
            @NotNull StatementType statementType, @NotNull StatementPublishType publishType) {
        return generationWriteServicesMap.get(buildTypeKey(productType, statementType, publishType));
    }

    @NotNull
    public AccountStatementGenerationWriteService getAccountStatementGenerationWriteService(@NotNull PortfolioProductType productType,
            @NotNull StatementType statementType, @NotNull StatementPublishType publishType) {
        AccountStatementGenerationWriteService service = findAccountStatementGenerationWriteService(productType, statementType,
                publishType);
        if (service == null) {
            throw new PlatformServiceUnavailableException("err.msg.statement.generation.write.service.implementation.missing",
                    SERVICE_MISSING + productType + "." + statementType + "." + publishType, productType, statementType, publishType);
        }
        return service;
    }

    public AccountStatementPublishReadService findAccountStatementPublishReadService(@NotNull PortfolioProductType productType) {
        return publishReadServicesMap.get(productType);
    }

    @NotNull
    public AccountStatementPublishReadService getAccountStatementPublishReadService(@NotNull PortfolioProductType productType) {
        AccountStatementPublishReadService service = findAccountStatementPublishReadService(productType);
        if (service == null) {
            throw new PlatformServiceUnavailableException("err.msg.statement.publish.read.service.implementation.missing",
                    SERVICE_MISSING + productType, productType);
        }
        return service;
    }

    public AccountStatementPublisher findAccountStatementPublishWriteService(@NotNull PortfolioProductType productType,
            @NotNull StatementType statementType, @NotNull StatementPublishType publishType) {
        return publishWriteServicesMap.get(buildTypeKey(productType, statementType, publishType));
    }

    @NotNull
    public AccountStatementPublisher getAccountStatementPublishWriteService(@NotNull PortfolioProductType productType,
            @NotNull StatementType statementType, @NotNull StatementPublishType publishType) {
        AccountStatementPublisher service = findAccountStatementPublishWriteService(productType, statementType, publishType);
        if (service == null) {
            throw new PlatformServiceUnavailableException("err.msg.statement.publish.service.implementation.missing",
                    SERVICE_MISSING + productType + "." + statementType + "." + publishType, productType, statementType, publishType);
        }
        return service;
    }

    public AccountStatementWriteService findAccountStatementWriteService(@NotNull PortfolioProductType productType) {
        return statementWriteServicesMap.get(productType);
    }

    @NotNull
    public AccountStatementWriteService getAccountStatementWriteService(@NotNull PortfolioProductType productType) {
        AccountStatementWriteService service = findAccountStatementWriteService(productType);
        if (service == null) {
            throw new PlatformServiceUnavailableException("err.msg.statement.service.implementation.missing", SERVICE_MISSING + productType,
                    productType);
        }
        return service;
    }

    private String buildTypeKey(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType) {
        return productType.getCode() + '.' + statementType.getCode() + '.' + publishType.getCode();
    }
}
