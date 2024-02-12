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
package org.apache.fineract.currentaccount.domain.account;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.apache.fineract.currentaccount.enumeration.product.BalanceCalculationType;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.service.MathUtil;

public interface ICurrentAccount {

    String getId();

    String getAccountNumber();

    ExternalId getExternalId();

    Long getClientId();

    String getProductId();

    @NotNull
    CurrentAccountStatus getStatus();

    LocalDate getActivatedOnDate();

    boolean isAllowOverdraft();

    BigDecimal getOverdraftLimit();

    boolean isAllowForceTransaction();

    BigDecimal getMinimumRequiredBalance();

    @NotNull
    BalanceCalculationType getBalanceCalculationType();

    default BigDecimal getAvailableBalance(@NotNull BigDecimal available, boolean addOverdraft) {
        BigDecimal minimumRequiredBalance = getMinimumRequiredBalance();
        available = MathUtil.subtract(available, minimumRequiredBalance);
        if (!addOverdraft || !isAllowOverdraft() || !MathUtil.isEmpty(minimumRequiredBalance)) {
            return available;
        }
        BigDecimal overdraftLimit = getOverdraftLimit();
        return overdraftLimit == null ? null : MathUtil.add(available, overdraftLimit);
    }

    default boolean isEnabled(CurrentAccountAction action) {
        return getStatus().isEnabled(action);
    }

    default void checkEnabled(CurrentAccountAction action) {
        getStatus().checkEnabled(action);
    }

    default CurrentAccountStatus getNextStatus(@NotNull CurrentAccountAction action) {
        return getStatus().getNextStatus(action);
    }

    default boolean hasBalanceDelay(@NotNull CurrentAccountAction action) {
        return getBalanceCalculationType().hasDelay(action);
    }

    default boolean isBalancePersist(@NotNull CurrentAccountAction action) {
        return getBalanceCalculationType().isPersist(action);
    }
}
