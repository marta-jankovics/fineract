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

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static org.apache.fineract.statement.data.camt053.AccountBalanceData.BALANCE_CODE_END_OF_PERIOD;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StatementData {

    @NotNull
    @JsonProperty(value = "Identification", required = true)
    private final String identification;
    @JsonProperty("CreationDateTime")
    @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private final OffsetDateTime creationDateTime;
    @JsonProperty("FromToDate")
    private final DateTimePeriodData fromToDate;
    @NotNull
    @JsonProperty(value = "Account", required = true)
    private final AccountData account;
    @NotNull
    @JsonProperty(value = "Balance", required = true)
    private final AccountBalanceData[] balances;
    @JsonProperty("TransactionsSummary")
    private final TransactionsSummaryData transactionsSummary;
    @JsonInclude(NON_EMPTY)
    @JsonProperty("Entry")
    private final TransactionStatementData[] transactions;
    @JsonProperty("AdditionalStatementInformation")
    private final String additionalStatementInformation;

    @Transient
    @JsonIgnore
    private final transient String accountType;
    @Transient
    @JsonIgnore
    private final transient String customerId;
    @Transient
    @JsonIgnore
    private final transient String accountId;
    @Transient
    @JsonIgnore
    private final transient String iban;
    @Transient
    @JsonIgnore
    private final transient LocalDate fromDate;
    @Transient
    @JsonIgnore
    private final transient LocalDate toDate;

    @Transient
    @JsonIgnore
    public BigDecimal getClosureBalance() {
        return Arrays.stream(getBalances()).filter(e -> BALANCE_CODE_END_OF_PERIOD.equals(e.getType().getCodeOrProprietary().getCode()))
                .findFirst().map(accountBalanceData -> accountBalanceData.getAmount().getAmount()).orElse(BigDecimal.ZERO);
    }

    @Transient
    @JsonIgnore
    public String getAccountType() {
        return accountType;
    }

    @Transient
    @JsonIgnore
    public String getCustomerId() {
        return customerId;
    }

    @Transient
    @JsonIgnore
    public String getAccountId() {
        return accountId;
    }

    @Transient
    @JsonIgnore
    public String getIban() {
        return iban;
    }

    @Transient
    @JsonIgnore
    public LocalDate getFromDate() {
        return fromDate;
    }

    @Transient
    @JsonIgnore
    public LocalDate getToDate() {
        return toDate;
    }
}
