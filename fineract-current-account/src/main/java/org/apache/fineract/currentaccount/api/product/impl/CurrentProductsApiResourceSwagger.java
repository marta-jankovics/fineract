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

public final class CurrentProductsApiResourceSwagger {

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
        public Boolean allowOverdraft;
        @Schema(example = "1000")
        public BigDecimal overdraftLimit;
        @Schema(example = "true")
        public Boolean enforceMinRequiredBalance;
        @Schema(example = "100")
        public BigDecimal minRequiredBalance;

        private PostCurrentProductRequest() {}
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
        public Boolean allowOverdraft;
        @Schema(example = "1000")
        public BigDecimal overdraftLimit;
        @Schema(example = "true")
        public Boolean enforceMinRequiredBalance;
        @Schema(example = "100")
        public BigDecimal minRequiredBalance;

        private PutCurrentProductRequest() {}
    }
}
