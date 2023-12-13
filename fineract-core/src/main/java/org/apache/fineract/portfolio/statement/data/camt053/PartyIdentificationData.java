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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.fineract.portfolio.statement.StatementUtils;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PartyIdentificationData {

    @JsonProperty("Name")
    @Size(min = 1, max = 140)
    private final String name;
    @JsonProperty("PostalAddress")
    private final PostalAddressData address;

    public static PartyIdentificationData create(String name, String address) {
        name = StatementUtils.ensureSize(name, "Name", 1, 140);
        PostalAddressData addressLine = PostalAddressData.create(address);
        if (name == null && address == null) {
            return null;
        }
        return new PartyIdentificationData(name, addressLine);
    }
}
