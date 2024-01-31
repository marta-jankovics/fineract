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
package org.apache.fineract.portfolio.savings.statement.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.apache.fineract.statement.data.camt053.Camt053Data;
import org.apache.fineract.statement.data.camt053.GroupHeaderData;
import org.apache.fineract.statement.data.camt053.StatementData;
import org.apache.logging.log4j.util.Strings;

@Getter
public class SavingsCamt053Data extends Camt053Data {

    public SavingsCamt053Data(@NotNull GroupHeaderData groupHeader) {
        super(groupHeader);
    }

    @Transient
    @SuppressFBWarnings("NP_BOOLEAN_RETURN_NULL")
    @JsonIgnore
    public Boolean isConversionAccount() {
        Boolean result = null;
        for (StatementData statement : getStatements()) {
            boolean conversionAccount = ((SavingsStatementData) statement).isConversionAccount();
            if (result == null) {
                result = conversionAccount;
            }
            if (result != conversionAccount) {
                return null;
            }
        }
        return result;
    }

    public JsonNode mapToJson(JsonMapper mapper) throws JsonProcessingException {
        JsonNode json = mapper.valueToTree(this);
        JsonNode statements = json.get("Statement");
        if (statements != null) {
            int stmIdx = 0;
            for (JsonNode statement : statements) {
                StatementData statementToAdd = getStatements()[stmIdx];
                JsonNode entries = statement.get("Entry");
                if (entries != null) {
                    int entIdx = 0;
                    for (JsonNode entry : entries) {
                        String detailsToAddS = ((SavingsTransactionData) statementToAdd.getTransactions()[entIdx])
                                .getStructuredEntryDetails();
                        if (!Strings.isEmpty(detailsToAddS)) {
                            ArrayNode detailsToAdd = (ArrayNode) mapper.readTree(detailsToAddS);
                            JsonNode entryDetails = entry.get("EntryDetails");
                            if (entryDetails == null) {
                                ((ObjectNode) entry).set("EntryDetails", detailsToAdd);
                            } else {
                                ((ArrayNode) entryDetails).addAll(detailsToAdd);
                            }
                        }
                        entIdx++;
                    }
                }
                stmIdx++;
            }
        }
        return json;
    }

    public String mapToString(JsonMapper mapper) throws JsonProcessingException {
        return mapper.writeValueAsString(mapToJson(mapper));
    }
}
