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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.fineract.portfolio.statement.data.camt053.EnvelopeData;

@Getter
@AllArgsConstructor
public class SupplementaryEnvelopeData extends EnvelopeData {

    @JsonProperty("SubscriptionPackage")
    private final String subscriptionPackage;
    @JsonProperty("CustomerShortName")
    private final String customerShortName;
    @JsonProperty("CustomerAddress")
    private final String customerAddress;

    public static SupplementaryEnvelopeData create(HashMap<String, Object> clientDetails) {
        if (clientDetails == null || clientDetails.isEmpty()) {
            return null;
        }
        return new SupplementaryEnvelopeData((String) clientDetails.get("subscription_package"), (String) clientDetails.get("short_name"),
                (String) clientDetails.get("address"));
    }
}
