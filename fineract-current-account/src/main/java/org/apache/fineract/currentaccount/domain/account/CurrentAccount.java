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

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.eclipselink.converter.UUIDConverter;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.portfolio.accountdetails.domain.AccountType;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.Converter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "m_current_account", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "account_no" }, name = "m_current_account_account_no_key"),
        @UniqueConstraint(columnNames = { "external_id" }, name = "m_current_account_external_id_key") })
@Converter(name = "uuidConverter", converterClass = UUIDConverter.class)
public class CurrentAccount extends AbstractAuditableWithUTCDateTimeCustom<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter(onMethod = @__(@Override))
    @Convert(value = "uuidConverter")
    private UUID id;

    @Column(name = "account_no", nullable = false, length = 50, unique = true)
    private String accountNo;
    @Column(name = "external_id", length = 100, unique = true)
    private ExternalId externalId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CurrentAccountStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "submitted_on_date", nullable = false)
    private LocalDate submittedOnDate;

    @Column(name = "submitted_by_user_id", nullable = false)
    private Long submittedByUserId;

    @Column(name = "cancelled_on_date")
    private LocalDate cancelledOnDate;

    @Column(name = "cancelled_by_user_id")
    private Long cancelledByUserId;

    @Column(name = "activated_on_date")
    private LocalDate activatedOnDate;

    @Column(name = "activated_by_user_id")
    private Long activatedByUserId;

    @Column(name = "closed_on_date")
    private LocalDate closedOnDate;

    @Column(name = "closed_by_user_id")
    private Long closedByUserId;
    @Embedded
    private MonetaryCurrency currency;

    @Column(name = "allow_overdraft", nullable = false)
    private boolean allowOverdraft;

    @Column(name = "overdraft_limit", precision = 6)
    private BigDecimal overdraftLimit;

    @Column(name = "enforce_min_required_balance")
    private boolean enforceMinRequiredBalance;

    @Column(name = "min_required_balance", precision = 6)
    private BigDecimal minRequiredBalance;

    @Version
    private Long version;

    public static CurrentAccount newInstanceForSubmit(Long clientId, UUID productId, String accountNo, MonetaryCurrency currency,
            ExternalId externalId, AccountType accountType, LocalDate submittedOnDate, Long submittedById, boolean allowOverdraft,
            BigDecimal overdraftLimit, boolean enforceMinRequiredBalance, BigDecimal minRequiredBalance) {

        CurrentAccount currentAccount = new CurrentAccount();
        currentAccount.setClientId(clientId);
        currentAccount.setProductId(productId);
        currentAccount.setAccountNo(accountNo);
        currentAccount.setStatus(CurrentAccountStatus.SUBMITTED);
        currentAccount.setExternalId(externalId);
        currentAccount.setAccountType(accountType);
        currentAccount.setSubmittedOnDate(submittedOnDate);
        currentAccount.setSubmittedByUserId(submittedById);
        currentAccount.setAllowOverdraft(allowOverdraft);
        currentAccount.setOverdraftLimit(overdraftLimit);
        currentAccount.setEnforceMinRequiredBalance(enforceMinRequiredBalance);
        currentAccount.setMinRequiredBalance(minRequiredBalance);
        currentAccount.setCurrency(currency);

        return currentAccount;
    }
}
