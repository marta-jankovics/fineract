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
package org.apache.fineract.portfolio.statement.data.camt053;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AccountIdentificationData {

    @JsonProperty("IBAN")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "^[A-Z]{2,2}[0-9]{2,2}[a-zA-Z0-9]{1,30}$")
    private final String iban;
    @JsonProperty("Other")
    private final IdentificationData other;

    public static AccountIdentificationData create(String iban, String other) {
        IdentificationData idData = iban != null ? null : IdentificationData.create(other);
        if (iban == null && idData == null) {
            return null;
        }
        return new AccountIdentificationData(iban, idData);
    }
}