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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.fineract.statement.StatementUtils;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IdentificationData {

    @JsonProperty("Identification")
    @Size(min = 1, max = 34)
    private final String identification;
    @JsonProperty("SchemeName")
    private final CodeOrProprietaryData scheme;

    public static IdentificationData create(String identification, CodeOrProprietaryData scheme) {
        identification = StatementUtils.ensureSize(identification, "Identification", 1, 34);
        return identification == null ? null : new IdentificationData(identification, scheme);
    }
}
