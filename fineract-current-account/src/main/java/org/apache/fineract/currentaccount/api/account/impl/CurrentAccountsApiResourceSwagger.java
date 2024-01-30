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
package org.apache.fineract.currentaccount.api.account.impl;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.fineract.currentaccount.api.common.CommonApiResourceSwagger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class CurrentAccountsApiResourceSwagger {

    private CurrentAccountsApiResourceSwagger() {
    }

    @Schema(description = "CurrentAccountSubmitRequest")
    public static final class PostCurrentAccountSubmitRequest {

        @Schema(example = "1")
        public Long clientId;
        @Schema(example = "1")
        public String productId;
        @Schema(example = "asdasd12")
        public String accountNumber;
        @Schema(example = "en")
        public String locale;
        @Schema(example = "dd MMMM yyyy")
        public String dateFormat;
        @Schema(example = "01 March 2011")
        public String submittedOnDate;
        @Schema(example = "11436b17-c690-4a30-8505-42a2c4eafb9d")
        public String externalId;
        @Schema(example = "false")
        public Boolean allowForceTransaction;
        @Schema(example = "10")
        public BigDecimal minimumRequiredBalance;
        @Schema(example = "false")
        public Boolean allowOverdraft;
        @Schema(example = "1000")
        public BigDecimal overdraftLimit;
        @Schema(example = "LAZY")
        public String balanceCalculationType;
        @Schema(description = "Datatable details")
        public List<CommonApiResourceSwagger.DatatableEntriesRequest> datatables;
        @Schema(description = "Alternative identifiers")
        public List<CurrentAccountIdentifiers> identifiers;
        private PostCurrentAccountSubmitRequest() {
        }
    }

    @Schema(description = "CurrentAccountUpdateRequest")
    public static final class PutCurrentAccountUpdateRequest {

        @Schema(example = "en")
        public String locale;
        @Schema(example = "asdasd12")
        public String accountNumber;
        @Schema(example = "11436b17-c690-4a30-8505-42a2c4eafb9d")
        public String externalId;
        @Schema(example = "false")
        public Boolean allowForceTransaction;
        @Schema(example = "10")
        public BigDecimal minimumRequiredBalance;
        @Schema(example = "false")
        public Boolean allowOverdraft;
        @Schema(example = "1000")
        public BigDecimal overdraftLimit;
        @Schema(example = "LAZY")
        public String balanceCalculationType;
        @Schema(description = "Datatable details")
        public List<CommonApiResourceSwagger.DatatableEntriesRequest> datatables;
        @Schema(description = "Alternative identifiers")
        public List<CurrentAccountIdentifiers> identifiers;
        private PutCurrentAccountUpdateRequest() {
        }
    }

    @Schema(description = "CurrentAccountActionRequest")
    public static final class PostCurrentAccountActionRequest {

        @Schema(example = "dd MMMM yyyy")
        public String dateFormat;
        @Schema(example = "en")
        public String locale;
        @Schema(example = "05 September 2014")
        public String actionDate;
        private PostCurrentAccountActionRequest() {
        }
    }

    @Schema(description = "CurrentAccountCommandResponse")
    public static class CurrentAccountCommandResponse {

        @Schema(example = "1")
        public Long clientId;
        @Schema(example = "7GGBmEwPEf6WgTchzDHnX")
        public String resourceIdentifier;
        @Schema(example = "95174ff9-1a75-4d72-a413-6f9b1cb988b7")
        public String resourceExternalId;

        private CurrentAccountCommandResponse() {}
    }

    @Schema(description = "CurrentAccountUpdateCommandResponse")
    public static final class CurrentAccountUpdateCommandResponse extends CurrentAccountCommandResponse {
        @Schema(example = "")
        public Map<String, Object> changes;

        private CurrentAccountUpdateCommandResponse() {}
    }

    @Schema(description = "CurrentAccountIdentifiers")
    public static final class CurrentAccountIdentifiers {
        @Schema(example = "iban")
        public String idType;
        @Schema(example = "123456789")
        public String value;
        @Schema(example = "A")
        public String subValue;
    }
}
