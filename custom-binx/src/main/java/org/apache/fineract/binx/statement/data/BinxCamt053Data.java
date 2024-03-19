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

import static org.apache.fineract.binx.BinxConstants.CONVERSION_ACCOUNT_DISCRIMINATOR;
import static org.apache.fineract.binx.BinxConstants.DISPOSAL_ACCOUNT_DISCRIMINATOR;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.apache.fineract.statement.data.camt053.Camt053Data;
import org.apache.fineract.statement.data.camt053.GroupHeaderData;

@Getter
public class BinxCamt053Data extends Camt053Data {

    public BinxCamt053Data(@NotNull GroupHeaderData groupHeader) {
        super(groupHeader);
    }

    @Transient
    @JsonIgnore
    public boolean isConversionAccount() {
        return CONVERSION_ACCOUNT_DISCRIMINATOR.equals(getAccountType());
    }

    @Transient
    @JsonIgnore
    public boolean isDisposalAccount() {
        return DISPOSAL_ACCOUNT_DISCRIMINATOR.equals(getAccountType());
    }
}
