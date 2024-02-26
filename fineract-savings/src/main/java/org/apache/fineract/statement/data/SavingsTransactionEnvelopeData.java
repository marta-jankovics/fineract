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
package org.apache.fineract.statement.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.fineract.statement.data.camt053.EnvelopeData;
import org.apache.fineract.statement.data.camt053.RelatedAccountData;
import org.apache.fineract.statement.data.camt053.TransactionPartiesData;
import org.apache.logging.log4j.util.Strings;

@Getter
@AllArgsConstructor
public class SavingsTransactionEnvelopeData extends EnvelopeData {

    @JsonProperty("OtherIdentification")
    private final TransactionPartiesData relatedParties;

    public static SavingsTransactionEnvelopeData create(String debtorIdentification, String debtorScheme, String creditorIdentification,
            String creditorScheme) {
        if (Strings.isEmpty(debtorIdentification) && Strings.isEmpty(creditorIdentification)) {
            return null;
        }
        TransactionPartiesData parties = new TransactionPartiesData(null,
                RelatedAccountData.create(null, debtorIdentification, debtorScheme, null), null,
                RelatedAccountData.create(null, creditorIdentification, creditorScheme, null));
        return new SavingsTransactionEnvelopeData(parties);
    }
}
