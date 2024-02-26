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
package org.apache.fineract.statement.data.camt053;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.fineract.statement.StatementUtils;

public class TransactionDetailsData {

    @JsonProperty("References")
    private final TransactionReferencesData references;
    @JsonProperty("RelatedParties")
    private final TransactionPartiesData relatedParties;
    @JsonProperty("RemittanceInformation")
    private final RemittanceInfoData remittanceInfo;
    @JsonProperty("AdditionalTransactionInformation")
    @Size(min = 1, max = 35)
    private final String additionalInfo;
    @JsonProperty("SupplementaryData")
    private SupplementaryData[] supplementaryDatas;

    public TransactionDetailsData(TransactionReferencesData references, TransactionPartiesData relatedParties,
            RemittanceInfoData remittanceInfo, String additionalInfo, SupplementaryData[] supplementaryDatas) {
        this.references = references;
        this.relatedParties = relatedParties;
        this.remittanceInfo = remittanceInfo;
        this.additionalInfo = StatementUtils.ensureSize(additionalInfo, "AdditionalTransactionInformation", 1, 35);
        this.supplementaryDatas = supplementaryDatas;
    }

    public void add(SupplementaryData supplementaryData) {
        if (supplementaryData != null) {
            supplementaryDatas = ArrayUtils.add(supplementaryDatas, supplementaryData);
        }
    }
}
