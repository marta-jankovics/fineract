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

import static org.apache.fineract.infrastructure.core.service.DateUtils.DEFAULT_DATE_FORMAT;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;

public abstract class StatementMetadata {

    @JsonProperty("Customer-Id")
    private String[] customerIds;
    @JsonProperty("Internal-Account-Id")
    private String[] accountIds;
    @JsonProperty("Account-Iban")
    private String[] ibans;
    @JsonProperty("Account-Type")
    private String[] accountTypes;
    @JsonProperty("Currency")
    private String[] currencies;
    @JsonProperty("Statement-From")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DEFAULT_DATE_FORMAT)
    private LocalDate[] fromDates;
    @JsonProperty("Statement-To")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DEFAULT_DATE_FORMAT)
    private LocalDate[] toDates;

    public void add(String customerId, String accountId, String iban, String accountType, @NotNull String currency,
            @NotNull LocalDate fromDate, @NotNull LocalDate toDate) {
        customerIds = ArrayUtils.add(customerIds, customerId);
        accountIds = ArrayUtils.add(accountIds, accountId);
        ibans = ArrayUtils.add(ibans, iban);
        accountTypes = ArrayUtils.add(accountTypes, accountType);
        currencies = ArrayUtils.add(currencies, currency);
        fromDates = ArrayUtils.add(fromDates, fromDate);
        toDates = ArrayUtils.add(toDates, toDate);
    }

    public void emptyToNull() {
        customerIds = emptyToNull(customerIds);
        accountIds = emptyToNull(accountIds);
        ibans = emptyToNull(ibans);
    }

    protected String[] emptyToNull(String[] values) {
        return values == null || values.length == 0 || Arrays.stream(values).filter(e -> e != null && !e.isBlank()).findAny().isEmpty()
                ? null
                : values;
    }

}
