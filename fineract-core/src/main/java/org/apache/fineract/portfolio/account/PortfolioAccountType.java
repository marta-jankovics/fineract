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
package org.apache.fineract.portfolio.account;

import lombok.Getter;

@Getter
public enum PortfolioAccountType {

    INVALID(0, "accountType.invalid"), //
    LOAN(1, "accountType.loan"), //
    SAVINGS(2, "accountType.savings"), //
    CURRENT(3, "accountType.current"), //
    ;

    private final Integer value;
    private final String code;

    PortfolioAccountType(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public static PortfolioAccountType fromInt(final Integer type) {

        PortfolioAccountType enumType = INVALID;
        if (type != null) {
            enumType = switch (type) {
                case 1 -> LOAN;
                case 2 -> SAVINGS;
                case 3 -> CURRENT;
                default -> INVALID;
            };
        }
        return enumType;
    }

    // TODO: bad practice and unnecessary code! why not just use the enum values themselves!?!
    public boolean isSavingsAccount() {
        return this.equals(SAVINGS);
    }

    // TODO: bad practice and unnecessary code! why not just use the enum values themselves!?!
    public boolean isLoanAccount() {
        return this.equals(LOAN);
    }
}
