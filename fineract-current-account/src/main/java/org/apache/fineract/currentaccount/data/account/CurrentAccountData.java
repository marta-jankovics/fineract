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
package org.apache.fineract.currentaccount.data.account;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;
import org.apache.fineract.currentaccount.enums.account.CurrentAccountStatus;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.portfolio.accountdetails.domain.AccountType;

@Data
public class CurrentAccountData implements Serializable {

    // Current product data
    private final UUID id;
    private final String accountNo;
    private final ExternalId externalId;
    private final Long clientId;
    private final UUID productId;
    private final CurrentAccountStatus status;
    private final AccountType accountType;
    private final LocalDate submittedOnDate;
    private final Long submittedOnUserId;
    private final LocalDate cancelledOnDate;
    private final Long cancelledOnUserId;
    private final LocalDate activatedOnDate;
    private final Long activatedOnUserId;
    private final LocalDate closedOnDate;
    private final Long closedOnUserId;
    private final String currencyCode;
    private final Integer digitsAfterDecimal;
    private final Integer inMultiplesOf;
    private final Boolean allowOverdraft;
    private final BigDecimal overdraftLimit;
    private final Boolean enforceMinRequiredBalance;
    private final BigDecimal minRequiredBalance;

    // Currency data
    private final String currencyName;
    private final String currencyNameCode;
    private final String currencyDisplaySymbol;

}
