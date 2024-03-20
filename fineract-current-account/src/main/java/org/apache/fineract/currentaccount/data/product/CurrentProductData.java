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
package org.apache.fineract.currentaccount.data.product;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.currentaccount.enumeration.product.BalanceCalculationType;
import org.apache.fineract.infrastructure.core.domain.ExternalId;

@Data
public class CurrentProductData implements Serializable {

    // Current product data
    private final String id;
    private final ExternalId externalId;
    private final String name;
    private final String shortName;
    private final String description;
    private final AccountingRuleType accountingType;
    private final boolean allowOverdraft;
    private final BigDecimal overdraftLimit;
    private final BigDecimal minimumRequiredBalance;
    private final boolean allowForceTransaction;
    private final BalanceCalculationType balanceCalculationType;

    // Currency data
    private final String currencyCode;
    private final Integer currencyDigitsAfterDecimal;
    private final Integer currencyInMultiplesOf;
    private final String currencyName;
    private final String currencyDisplaySymbol;
}