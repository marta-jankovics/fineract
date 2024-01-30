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
import java.util.List;
import lombok.Data;
import org.apache.fineract.accounting.glaccount.data.GLAccountDataForLookup;
import org.apache.fineract.infrastructure.core.data.StringEnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;

@Data
public class CurrentProductResponseData implements Serializable {

    // Current product data
    private final String id;
    private final String name;
    private final String shortName;
    private final String description;
    private final CurrencyData currency;
    private final StringEnumOptionData accountingType;
    private final Boolean allowOverdraft;
    private final BigDecimal overdraftLimit;
    private final BigDecimal minimumRequiredBalance;
    private final Boolean allowForceTransaction;
    private final StringEnumOptionData balanceCalculationType;

    private GLAccountDataForLookup controlAccountId;
    private GLAccountDataForLookup referenceAccountId;
    private GLAccountDataForLookup overdraftAccountId;
    private GLAccountDataForLookup transfersInSuspenseAccountId;
    private GLAccountDataForLookup writeOffAccountId;
    private GLAccountDataForLookup incomeFromFee;
    private GLAccountDataForLookup incomeFromPenalty;
    private List<PaymentChannelToFundSourceData> paymentChannelToFundSourceMappings;
}
