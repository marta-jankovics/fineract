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
package org.apache.fineract.statement.data.camt053;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.service.MathUtil;

@Getter
@AllArgsConstructor
public class AccountBalanceData {

    public static final String BALANCE_CODE_END_OF_PERIOD = "CLBD";
    public static final String BALANCE_CODE_FULL_OF_PERIOD = "XPCD";
    public static final String BALANCE_CODE_BEGIN_OF_PERIOD = "OPBD";

    @NotNull
    @JsonProperty(value = "Type", required = true)
    private final BalanceTypeData type;
    @NotNull
    @JsonProperty(value = "Amount", required = true)
    private final BalanceAmountData amount;
    @NotNull
    @JsonProperty(value = "CreditDebitIndicator", required = true)
    private final CreditDebitIndicator creditDebitIndicator;
    @NotNull
    @JsonProperty(value = "Date", required = true)
    private final DateAndTimeData date;

    public static AccountBalanceData create(@NotNull String code, @NotNull BigDecimal amount, @NotNull String currency,
            @NotNull LocalDate date) {
        CreditDebitIndicator crdDbt = MathUtil.isLessThanZero(amount) ? CreditDebitIndicator.DBIT : CreditDebitIndicator.CRDT;
        amount = MathUtil.abs(amount);
        return new AccountBalanceData(BalanceTypeData.create(code), new BalanceAmountData(amount, currency), crdDbt,
                new DateAndTimeData(date));
    }
}
