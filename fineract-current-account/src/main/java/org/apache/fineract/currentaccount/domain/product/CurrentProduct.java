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
package org.apache.fineract.currentaccount.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.currentaccount.enumeration.product.BalanceCalculationType;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "m_current_product", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }, name = "m_current_product_name_key"),
        @UniqueConstraint(columnNames = { "short_name" }, name = "m_current_product_short_name_key"),
        @UniqueConstraint(columnNames = { "external_id" }, name = "m_current_product_external_id_key") })
public class CurrentProduct extends AbstractAuditableWithUTCDateTimeCustom<String> {

    // TODO CURRENT! interface for string id entities
    @Id
    @GeneratedValue(generator = "nanoIdSequence")
    @Getter(onMethod = @__(@Override))
    private String id;

    @Column(name = "external_id", length = 100, unique = true)
    private ExternalId externalId;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "short_name", nullable = false, length = 8, unique = true)
    private String shortName;

    @Column(name = "description", length = 500)
    private String description;

    @Embedded
    private MonetaryCurrency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "accounting_type", nullable = false)
    private AccountingRuleType accountingType;

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

    public CurrentProduct(ExternalId externalId, String name, String shortName, String description, MonetaryCurrency currency,
            AccountingRuleType accountingType, boolean allowOverdraft, BigDecimal overdraftLimit, boolean allowForceTransaction,
            BigDecimal minimumRequiredBalance, BalanceCalculationType balanceCalculationType) {
        this.externalId = externalId;
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.currency = currency;
        this.accountingType = accountingType;
        this.allowOverdraft = allowOverdraft;
        this.overdraftLimit = overdraftLimit;
        this.allowForceTransaction = allowForceTransaction;
        this.minimumRequiredBalance = minimumRequiredBalance;
        this.balanceCalculationType = balanceCalculationType;
    }

    public void setExternalId(ExternalId externalId) {
        this.externalId = externalId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCurrency(MonetaryCurrency currency) {
        this.currency = currency;
    }

    public void setAccountingType(AccountingRuleType accountingType) {
        this.accountingType = accountingType;
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
}
