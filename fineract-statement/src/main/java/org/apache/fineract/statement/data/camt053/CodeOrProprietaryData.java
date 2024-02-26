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
import lombok.Getter;
import org.apache.fineract.statement.StatementUtils;

@Getter
public final class CodeOrProprietaryData {

    @JsonProperty("Code")
    @Size(min = 1, max = 4)
    private final String code;
    @JsonProperty("Proprietary")
    @Size(min = 1, max = 35)
    private final String proprietary;

    private CodeOrProprietaryData(String code, String proprietary) {
        this.code = StatementUtils.ensureSize(code, "Code", 1, 4);
        this.proprietary = StatementUtils.ensureSize(proprietary, "Proprietary", 1, 35);
    }

    public static CodeOrProprietaryData create(String code, String proprietary) {
        return (code == null && proprietary == null) ? null : new CodeOrProprietaryData(code, proprietary);
    }
}
