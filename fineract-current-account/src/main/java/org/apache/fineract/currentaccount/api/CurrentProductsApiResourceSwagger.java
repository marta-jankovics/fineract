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
package org.apache.fineract.currentaccount.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

final class CurrentProductsApiResourceSwagger {

    private CurrentProductsApiResourceSwagger() {}

    @Schema(description = "PostCurrentProductRequest")
    public static final class PostCurrentProductRequest {

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
        public boolean allowOverdraft;
        @Schema(example = "1000")
        public BigDecimal overdraftLimit;
        @Schema(example = "100")
        public BigDecimal minRequiredBalance;

        private PostCurrentProductRequest() {}
    }

    @Schema(description = "PostCurrentProductResponse")
    public static final class PostCurrentProductResponse {

        @Schema(example = "1")
        public Long resourceId;

        private PostCurrentProductResponse() {}
    }

    @Schema(description = "PutCurrentProductRequest")
    public static final class PutCurrentProductRequest {

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
        public boolean allowOverdraft;
        @Schema(example = "1000")
        public BigDecimal overdraftLimit;
        @Schema(example = "100")
        public BigDecimal minRequiredBalance;

        private PutCurrentProductRequest() {}
    }

    @Schema(description = "PutCurrentProductResponse")
    public static final class PutCurrentProductResponse {

        @Schema(example = "1")
        public Long resourceId;
        public PutCurrentProductChanges changes;

        private PutCurrentProductResponse() {}

        static final class PutCurrentProductChanges {

            private PutCurrentProductChanges() {}

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
            public boolean allowOverdraft;
            @Schema(example = "1000")
            public BigDecimal overdraftLimit;
            @Schema(example = "100")
            public BigDecimal minRequiredBalance;
        }
    }

    @Schema(description = "GetCurrentProductResponse")
    public static final class GetCurrentProductResponse {

        private GetCurrentProductResponse() {}

        @Schema(example = "1")
        public Integer id;
        @Schema(example = "current product")
        public String name;
        @Schema(example = "sa1")
        public String shortName;
        @Schema(example = "Some words about the product")
        public String description;
        @Schema(example = "true")
        public boolean allowOverdraft;
        @Schema(example = "1000")
        public BigDecimal overdraftLimit;
        @Schema(example = "10")
        public BigDecimal minRequiredBalance;
        public GetCurrency currency;
        public GetAccountingRule accountingRule;
    }

    @Schema(description = "GetCurrentProductTemplateResponse")
    public static final class GetCurrentProductTemplateResponse {

        private GetCurrentProductTemplateResponse() {}

        public List<GetCurrency> currencyOptions;
        public List<GetAccountingRule> accountingTypeOptions;
    }

    @Schema(description = "DeleteCurrentProductResponse")
    public static final class DeleteCurrentProductResponse {

        private DeleteCurrentProductResponse() {}

        @Schema(example = "1")
        public Long resourceId;
    }

    @Schema(description = "GetCurrency")
    public static final class GetCurrency {

        @Schema(example = "USD")
        public String code;
        @Schema(example = "US Dollar")
        public String name;
        @Schema(example = "2")
        public Integer decimalPlaces;
        @Schema(example = "$")
        public String displaySymbol;
        @Schema(example = "currency.USD")
        public String nameCode;
        @Schema(example = "US Dollar ($)")
        public String displayLabel;

        private GetCurrency() {}
    }

    @Schema(description = "GetAccountingRule")
    public static final class GetAccountingRule {

        private GetAccountingRule() {}

        @Schema(example = "1")
        public Integer id;
        @Schema(example = "accountingRuleType.none")
        public String code;
        @Schema(example = "NONE")
        public String value;
    }
}
