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
package org.apache.fineract.currentaccount.api.transaction.impl;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class CurrentTransactionsApiResourceSwagger {

    private CurrentTransactionsApiResourceSwagger() {}

    @Schema(description = "PostCurrentTransactionsRequest")
    public static final class PostCurrentTransactionsRequest {

        private PostCurrentTransactionsRequest() {}

        @Schema(example = "27 March 2022")
        public String transactionDate;
        @Schema(example = "1000")
        public BigDecimal transactionAmount;
        @Schema(example = "en")
        public String locale;
        @Schema(example = "dd MMMM yyyy")
        public String dateFormat;
        @Schema(example = "1")
        public Integer paymentTypeId;
        @Schema(description = "Datatable details")
        public List<DatatableEntriesRequest> datatables;
    }

    static final class DatatableEntriesRequest {

        private DatatableEntriesRequest() {}

        @Schema(example = "dt_test_datatable", description = "Name of the datatable")
        public String name;
        @Schema(description = "List of entries. An entry is a String column name-value map. For One To Many update entry, the 'id' name-value pair is mandatory.")
        public List<Map<String, String>> entries;
    }
}
