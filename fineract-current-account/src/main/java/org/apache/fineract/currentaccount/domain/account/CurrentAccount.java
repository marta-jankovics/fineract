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

import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus.SUBMITTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.apache.fineract.currentaccount.enumeration.product.BalanceCalculationType;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.service.MathUtil;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "m_current_account", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "account_no" }, name = "m_current_account_account_no_key"),
        @UniqueConstraint(columnNames = { "external_id" }, name = "m_current_account_external_id_key") })
public class CurrentAccount extends AbstractAuditableWithUTCDateTimeCustom<String> {

    @Id
    @GeneratedValue(generator = "nanoIdSequence")
    @Getter(onMethod = @__(@Override))
    private String id;

    @Column(name = "account_no", length = 50, unique = true)
    private String accountNumber;

    @Column(name = "external_id", length = 100, unique = true)
    private ExternalId externalId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_type", nullable = false)
    private CurrentAccountStatus status;

    @Column(name = "activated_on_date")
    private LocalDate activatedOnDate;

    @Column(name = "allow_overdraft", nullable = false)
    private boolean allowOverdraft;

    @Column(name = "overdraft_limit", precision = 6)
    private BigDecimal overdraftLimit;

    @Column(name = "allow_force_transaction", nullable = false)
    private boolean allowForceTransaction;

    @Column(name = "min_required_balance", precision = 6)
    private BigDecimal minimumRequiredBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "balance_calculation_type", nullable = false)
    private BalanceCalculationType balanceCalculationType;

    @Version
    private Long version;

    public CurrentAccount(String accountNumber, ExternalId externalId, Long clientId, String productId, CurrentAccountStatus status,
            boolean allowOverdraft, BigDecimal overdraftLimit, boolean allowForceTransaction, BigDecimal minimumRequiredBalance,
            BalanceCalculationType balanceCalculationType) {
        this.accountNumber = accountNumber;
        this.externalId = externalId;
        this.clientId = clientId;
        this.productId = productId;
        this.status = status;
        this.allowOverdraft = allowOverdraft;
        this.overdraftLimit = overdraftLimit;
        this.allowForceTransaction = allowForceTransaction;
        this.minimumRequiredBalance = minimumRequiredBalance;
        this.balanceCalculationType = balanceCalculationType;
    }

    public static CurrentAccount newInstanceForSubmit(String accountNumber, ExternalId externalId, Long clientId, String productId,
            boolean allowOverdraft, BigDecimal overdraftLimit, boolean allowForceTransaction, BigDecimal minimumRequiredBalance,
            BalanceCalculationType balanceCalculationType) {
        return new CurrentAccount(accountNumber, externalId, clientId, productId, SUBMITTED, allowOverdraft, overdraftLimit,
                allowForceTransaction, minimumRequiredBalance, balanceCalculationType);
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setExternalId(ExternalId externalId) {
        this.externalId = externalId;
    }

    public void setStatus(CurrentAccountStatus status) {
        this.status = status;
    }

    public void setActivatedOnDate(LocalDate activatedOnDate) {
        this.activatedOnDate = activatedOnDate;
    }

    public void setAllowOverdraft(boolean allowOverdraft) {
        this.allowOverdraft = allowOverdraft;
    }

    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }

    public void setAllowForceTransaction(boolean allowForceTransaction) {
        this.allowForceTransaction = allowForceTransaction;
    }

    public void setMinimumRequiredBalance(BigDecimal minimumRequiredBalance) {
        this.minimumRequiredBalance = minimumRequiredBalance;
    }

    public void setBalanceCalculationType(BalanceCalculationType balanceCalculationType) {
        this.balanceCalculationType = balanceCalculationType;
    }

    public BigDecimal getAvailableBalance(@NotNull BigDecimal available, boolean addOverdraft) {
        BigDecimal minimumRequiredBalance = getMinimumRequiredBalance();
        available = MathUtil.subtract(available, minimumRequiredBalance);
        if (!addOverdraft || !isAllowOverdraft() || !MathUtil.isEmpty(minimumRequiredBalance)) {
            return available;
        }
        BigDecimal overdraftLimit = getOverdraftLimit();
        return overdraftLimit == null ? null : MathUtil.add(available, overdraftLimit);
    }
}
