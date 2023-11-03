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
package org.apache.fineract.portfolio.savings.statement.data;

import static org.apache.fineract.infrastructure.core.service.DateUtils.DEFAULT_DATE_FORMAT;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.base.Strings;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.fineract.portfolio.statement.data.camt053.StatementMetaData;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SavingsMetaData extends StatementMetaData {

    public static final String CONVERSION_ACCOUNT = "conversion";
    public static final String DISPOSAL_ACCOUNT = "disposal";

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

    public void add(Map<String, Object> clientDetails, @NotNull Map<String, Object> accountDetails, boolean isConversionAccount,
            @NotNull String currency, @NotNull LocalDate fromDate, @NotNull LocalDate toDate) {
        customerIds = ArrayUtils.add(customerIds,
                Strings.nullToEmpty(clientDetails == null ? null : (String) clientDetails.get("customer_id")));
        accountIds = ArrayUtils.add(accountIds, Strings.nullToEmpty((String) accountDetails.get("internal_account_id")));
        ibans = ArrayUtils.add(ibans, Strings.nullToEmpty((String) accountDetails.get("iban")));
        accountTypes = ArrayUtils.add(accountTypes, isConversionAccount ? CONVERSION_ACCOUNT : DISPOSAL_ACCOUNT);
        currencies = ArrayUtils.add(currencies, currency);
        fromDates = ArrayUtils.add(fromDates, fromDate);
        toDates = ArrayUtils.add(toDates, toDate);
    }

    public String mapToString(JsonMapper mapper) throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }
}
