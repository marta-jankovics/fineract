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
package org.apache.fineract.currentaccount.api.account;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public final class CurrentAccountsApiResourceSwagger {

    private CurrentAccountsApiResourceSwagger() {}

    @Schema(description = "PostCurrentAccountSubmitRequest")
    public static final class PostCurrentAccountSubmitRequest {

        private PostCurrentAccountSubmitRequest() {}

        @Schema(example = "1")
        public Long clientId;
        @Schema(example = "1")
        public Long productId;
        @Schema(example = "asdasd12")
        public String accountNo;
        @Schema(example = "en")
        public String locale;
        @Schema(example = "dd MMMM yyyy")
        public String dateFormat;
        @Schema(example = "01 March 2011")
        public String submittedOnDate;
        @Schema(example = "11436b17-c690-4a30-8505-42a2c4eafb9d")
        public String externalId;
        @Schema(example = "false")
        public Boolean enforceMinRequiredBalance;
        @Schema(example = "10")
        public BigDecimal minimumRequiredBalance;
        @Schema(example = "false")
        public Boolean allowOverdraft;
        @Schema(example = "1000")
        public BigDecimal overdraftLimit;
    }

    @Schema(description = "PutCurrentAccountActionRequest")
    public static final class PutCurrentAccountActionRequest {

        private PutCurrentAccountActionRequest() {}

        @Schema(example = "en")
        public String locale;
        @Schema(example = "5.9999999999")
        public Double nominalAnnualInterestRate;
    }

    @Schema(description = "PostCurrentAccountActionRequest")
    public static final class PostCurrentAccountActionRequest {

        private PostCurrentAccountActionRequest() {}

        @Schema(example = "dd MMMM yyyy")
        public String dateFormat;
        @Schema(example = "en")
        public String locale;
        @Schema(example = "05 September 2014")
        public String approvedOnDate;
        @Schema(example = "05 September 2014")
        public String activatedOnDate;
    }
}
