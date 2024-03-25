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
package org.apache.fineract.binx.statement.data;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Getter;
import org.apache.fineract.statement.data.camt053.AccountData;
import org.apache.fineract.statement.data.camt053.AccountIdentificationData;
import org.apache.fineract.statement.data.camt053.PartyIdentificationData;

@Getter
public class BinxAccountData extends AccountData {

    public BinxAccountData(@NotNull AccountIdentificationData identification, String currency, PartyIdentificationData owner) {
        super(identification, currency, owner);
    }

    public static BinxAccountData create(String iban, String identification, String schemeProprietary, Map<String, Object> clientDetails,
            String currency) {
        AccountIdentificationData idData = AccountIdentificationData.create(iban, identification, schemeProprietary);
        if (idData == null) {
            return null;
        }
        PartyIdentificationData owner = null;
        if (clientDetails != null) {
            String name = (String) clientDetails.get("short_name");
            String address = (String) clientDetails.get("address");
            owner = PartyIdentificationData.create(name, address, null);
        }
        return new BinxAccountData(idData, currency, owner);
    }
}
