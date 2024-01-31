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
import org.apache.fineract.currentaccount.api.common.CommonApiResourceSwagger;

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
        public List<CommonApiResourceSwagger.DatatableEntriesRequest> datatables;

        // Accounting
        @Schema(example = "1")
        public Long controlAccountId;
        @Schema(example = "1")
        public Long referenceAccountId;
        @Schema(example = "1")
        public Long overdraftControlAccountId;
        @Schema(example = "1")
        public Long transfersInSuspenseAccountId;
        @Schema(example = "1")
        public Long writeOffAccountId;
        @Schema(example = "1")
        public Long incomeFromFeeAccountId;
        @Schema(example = "1")
        public Long incomeFromPenaltyAccountId;
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

    @Schema(description = "CurrentProductDeleteCommandResponse")
    public static final class CurrentProductDeleteCommandResponse extends CurrentProductCommandResponse {

    }

    @Schema(description = "CurrentProductCommandResponse")
    public static class CurrentProductCommandResponse {

        @Schema(example = "7GGBmEwPEf6WgTchzDHnX")
        public String resourceIdentifier;
        @Schema(example = "95174ff9-1a75-4d72-a413-6f9b1cb988b7")
        public String resourceExternalId;

        private CurrentProductCommandResponse() {}
    }

    @Schema(description = "CurrentProductUpdateCommandResponse")
    public static final class CurrentProductUpdateCommandResponse extends CurrentProductCommandResponse {

        @Schema(example = "")
        public Map<String, Object> changes;

        private CurrentProductUpdateCommandResponse() {}
    }
}
