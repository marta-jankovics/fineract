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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;

@Getter
public class Camt053Data {

    @NotNull
    @JsonProperty(value = "GroupHeader", required = true)
    private final GroupHeaderData groupHeader;
    @NotNull
    @JsonProperty(value = "Statement", required = true)
    private StatementData[] statements;
    @JsonInclude(NON_EMPTY)
    @JsonProperty("SupplementaryData")
    private SupplementaryData[] supplementaryDatas;

    public Camt053Data(@NotNull GroupHeaderData groupHeader) {
        this.groupHeader = groupHeader;
    }

    public void add(@NotNull StatementData statement, SupplementaryData supplementaryData) {
        statements = ArrayUtils.add(statements, statement);
        if (supplementaryData != null) {
            supplementaryDatas = ArrayUtils.add(supplementaryDatas, supplementaryData);
        }
    }
}
