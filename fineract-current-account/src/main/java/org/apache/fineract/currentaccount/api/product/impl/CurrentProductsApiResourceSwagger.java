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
package org.apache.fineract.currentaccount.api.product.impl;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class CurrentProductsApiResourceSwagger {

    private CurrentProductsApiResourceSwagger() {}

    @Schema(description = "CurrentProductRequest")
    public static final class CurrentProductRequest {

        @Schema(example = "Normal current product")
        public String name;
        @Schema(example = "NCP")
        public String shortName;
        @Schema(example = "A good old regular current product")
        public String description;
        @Schema(example = "USD")
        public String currencyCode;
        @Schema(example = "2")
        public Integer digitsAfterDecimal;
        @Schema(example = "0")
        public Integer inMultiplesOf;
        @Schema(example = "en")
        public String locale;
        @Schema(example = "1")
        public Integer accountingType;
        @Schema(example = "true")
        public Boolean allowOverdraft;
        @Schema(example = "1000")
        public BigDecimal overdraftLimit;
        @Schema(example = "true")
        public Boolean allowForceTransaction;
        @Schema(example = "100")
        public BigDecimal minimumRequiredBalance;
        @Schema(example = "LAZY")
        public String balanceCalculationType;
        @Schema(description = "Datatable details")
        public List<DatatableEntriesRequest> datatables;

        // Accounting
        @Schema(example = "1")
        public Long controlAccountId;
        @Schema(example = "1")
        public Long referenceAccountId;
        @Schema(example = "1")
        public Long overdraftAccountId;
        @Schema(example = "1")
        public Long transfersInSuspenseAccountId;
        @Schema(example = "1")
        public Long writeOffAccountId;
        @Schema(example = "1")
        public Long incomeFromFee;
        @Schema(example = "1")
        public Long incomeFromPenalty;
        @Schema(example = "")
        public List<PaymentChannelToFundSource> paymentChannelToFundSourceMappings;

        private CurrentProductRequest() {}

        public static final class PaymentChannelToFundSource {

            @Schema(example = "1")
            public Long paymentTypeId;
            @Schema(example = "1")
            public Long fundSourceAccountId;

            private PaymentChannelToFundSource() {}
        }
    }

    static final class DatatableEntriesRequest {

        private DatatableEntriesRequest() {}

        @Schema(example = "dt_test_datatable", description = "Name of the datatable")
        public String name;
        @Schema(description = "List of entries. An entry is a String column name-value map. For One To Many update entry, the 'id' name-value pair is mandatory.")
        public List<Map<String, String>> entries;
    }
}
