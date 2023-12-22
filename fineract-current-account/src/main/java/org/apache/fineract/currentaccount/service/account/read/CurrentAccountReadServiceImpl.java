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
package org.apache.fineract.currentaccount.service.account.read;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountResponseData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountTemplateResponseData;
import org.apache.fineract.currentaccount.data.product.CurrentProductResponseData;
import org.apache.fineract.currentaccount.exception.account.CurrentAccountNotFoundException;
import org.apache.fineract.currentaccount.mapper.account.CurrentAccountResponseDataMapper;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.service.product.read.CurrentProductReadService;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Slf4j
@RequiredArgsConstructor
public class CurrentAccountReadServiceImpl implements CurrentAccountReadService {

    private final CurrentAccountRepository currentAccountRepository;
    private final CurrentProductReadService currentProductReadPlatformService;
    private final CurrentAccountResponseDataMapper currentAccountResponseDataMapper;

    @Override
    public Page<CurrentAccountResponseData> retrieveAll(Pageable pageable) {
        return currentAccountResponseDataMapper.map(currentAccountRepository.findAllCurrentAccountData(pageable));
    }

    @Override
    public CurrentAccountResponseData retrieveById(final Long accountId) {
        CurrentAccountData currentAccountData = currentAccountRepository.findCurrentAccountData(accountId);
        if (currentAccountData == null) {
            throw new CurrentAccountNotFoundException(accountId);
        }
        return currentAccountResponseDataMapper.map(currentAccountData);
    }

    @Override
    public CurrentAccountTemplateResponseData retrieveTemplate() {
        final List<CurrentProductResponseData> productOptions = this.currentProductReadPlatformService
                .retrieveAll(Sort.by(Sort.Direction.ASC, "name"));

        return new CurrentAccountTemplateResponseData(productOptions);
    }

    @Override
    public CurrentAccountResponseData retrieveByExternalId(ExternalId externalId) {
        CurrentAccountResponseData currentAccountResponseData = currentAccountResponseDataMapper
                .map(currentAccountRepository.findCurrentAccountData(externalId));
        if (currentAccountResponseData == null) {
            throw new CurrentAccountNotFoundException(externalId);
        }
        return currentAccountResponseData;
    }

    @Override
    public Long retrieveAccountIdByExternalId(ExternalId accountExternalId) {
        Long id = currentAccountRepository.findIdByExternalId(accountExternalId);
        if (id == null) {
            throw new CurrentAccountNotFoundException(accountExternalId);
        }
        return id;
    }
}
