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
package org.apache.fineract.organisation.monetary.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepository;
import org.apache.fineract.organisation.monetary.domain.OrganisationCurrencyRepository;
import org.springframework.data.domain.Sort;

@RequiredArgsConstructor
public class CurrencyReadPlatformServiceImpl implements CurrencyReadPlatformService {

    private final PlatformSecurityContext context;
    private final ApplicationCurrencyRepository currencyRepository;
    private final OrganisationCurrencyRepository organisationCurrencyRepository;

    @Override
    public List<CurrencyData> retrieveAllowedCurrencies() {
        this.context.authenticatedUser();
        return organisationCurrencyRepository.findAllSorted(Sort.by("name"));
    }

    @Override
    public List<CurrencyData> retrieveAllPlatformCurrencies() {
        this.context.authenticatedUser();
        return currencyRepository.findAllSorted(Sort.by("name"));
    }

    @Override
    public CurrencyData retrieveCurrency(final String code) {
        this.context.authenticatedUser();
        return currencyRepository.findCurrencyDataByCode(code);
    }
}
