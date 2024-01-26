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

import lombok.Data;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.apache.fineract.currentaccount.enumeration.product.BalanceCalculationType;
import org.apache.fineract.infrastructure.core.domain.ExternalId;

@Data
public class CurrentAccountData implements Serializable {

    // Current account data
    private final String id;
    private final String accountNumber;
    private final ExternalId externalId;
    private final Long clientId;
    private final String productId;
    private final CurrentAccountStatus status;
    private final LocalDate activatedOnDate;
    private final Boolean allowOverdraft;
    private final BigDecimal overdraftLimit;
    private final Boolean allowForceTransaction;
    private final BigDecimal minimumRequiredBalance;
    private final BalanceCalculationType balanceCalculationType;

    // Current product data
    private final String currencyCode;
    private final Integer currencyDigitsAfterDecimal;
    private final Integer currencyInMultiplesOf;
    // Currency data
    private final String currencyName;
    private final String currencyDisplaySymbol;

}
