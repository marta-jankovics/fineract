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
package org.apache.fineract.currentaccount.repository.account;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountIdentifiersData;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentAccountRepository extends JpaRepository<CurrentAccount, String> {

    @Query("SELECT new org.apache.fineract.currentaccount.data.account.CurrentAccountData(ca.id, ca.accountNumber, ca.externalId, ca.clientId, c.displayName, c.office.id, ca.productId, cp.name, cp.shortName, cp.description, cp.accountingType, ca.status, ca.activatedOnDate, ca.allowOverdraft, ca.overdraftLimit, ca.allowForceTransaction, ca.minimumRequiredBalance, ca.balanceCalculationType, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, curr.name, curr.displaySymbol) FROM CurrentAccount ca, CurrentProduct cp, Client c, ApplicationCurrency curr WHERE ca.productId = cp.id AND curr.code = cp.currency.code AND ca.clientId = c.id AND ca.id = :id")
    CurrentAccountData getAccountDataById(@Param("id") String id);

    @Query("SELECT new org.apache.fineract.currentaccount.data.account.CurrentAccountData(ca.id, ca.accountNumber, ca.externalId, ca.clientId, c.displayName, c.office.id, ca.productId, cp.name, cp.shortName, cp.description, cp.accountingType, ca.status, ca.activatedOnDate, ca.allowOverdraft, ca.overdraftLimit, ca.allowForceTransaction, ca.minimumRequiredBalance, ca.balanceCalculationType, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, curr.name, curr.displaySymbol) FROM CurrentAccount ca, CurrentProduct cp, Client c, ApplicationCurrency curr WHERE ca.productId = cp.id AND curr.code = cp.currency.code  AND ca.clientId = c.id AND ca.externalId = :externalId")
    CurrentAccountData getAccountDataByExternalId(@Param("externalId") ExternalId externalId);

    @Query("SELECT new org.apache.fineract.currentaccount.data.account.CurrentAccountData(ca.id, ca.accountNumber, ca.externalId, ca.clientId, c.displayName, c.office.id, ca.productId, cp.name, cp.shortName, cp.description, cp.accountingType, ca.status, ca.activatedOnDate, ca.allowOverdraft, ca.overdraftLimit, ca.allowForceTransaction, ca.minimumRequiredBalance, ca.balanceCalculationType, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, curr.name, curr.displaySymbol) FROM CurrentAccount ca, CurrentProduct cp, Client c, ApplicationCurrency curr WHERE ca.productId = cp.id AND curr.code = cp.currency.code AND ca.clientId = c.id")
    Page<CurrentAccountData> getAccountsDataPage(Pageable pageable);

    @Query("SELECT ca.id FROM CurrentAccount ca WHERE ca.externalId = :externalId")
    Optional<String> findIdByExternalId(@Param("externalId") ExternalId externalId);

    @Query("SELECT ca.id FROM CurrentAccount ca WHERE ca.accountNumber = :accountNumber")
    Optional<String> findIdByAccountNumber(@Param("accountNumber") String accountNumber);

    @Query("SELECT new org.apache.fineract.currentaccount.data.account.CurrentAccountData(ca.id, ca.accountNumber, ca.externalId, ca.clientId, c.displayName, c.office.id, ca.productId, cp.name, cp.shortName, cp.description, cp.accountingType, ca.status, ca.activatedOnDate, ca.allowOverdraft, ca.overdraftLimit, ca.allowForceTransaction, ca.minimumRequiredBalance, ca.balanceCalculationType, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, curr.name, curr.displaySymbol) FROM CurrentAccount ca, CurrentProduct cp, ApplicationCurrency curr, Client c WHERE ca.productId = cp.id AND curr.code = cp.currency.code AND ca.clientId = c.id AND c.id = :clientId")
    List<CurrentAccountData> getAccountsDataByClientId(@Param("clientId") Long clientId, Sort sort);

    @Query("SELECT new org.apache.fineract.currentaccount.data.account.CurrentAccountData(ca.id, ca.accountNumber, ca.externalId, ca.clientId, c.displayName, c.office.id, ca.productId, cp.name, cp.shortName, cp.description, cp.accountingType, ca.status, ca.activatedOnDate, ca.allowOverdraft, ca.overdraftLimit, ca.allowForceTransaction, ca.minimumRequiredBalance, ca.balanceCalculationType, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, curr.name, curr.displaySymbol) FROM CurrentAccount ca, CurrentProduct cp, Client c, ApplicationCurrency curr WHERE ca.productId = cp.id AND curr.code = cp.currency.code AND ca.clientId = c.id  AND ca.accountNumber = :accountNumber")
    CurrentAccountData getAccountDataByAccountNumber(@Param("accountNumber") String accountNumber);

    @Query("select case when (count (ca) > 0) then 'true' else 'false' end from CurrentAccount ca where ca.clientId = :clientId and ca.status in :statuses")
    boolean hasAccountInStatusByClient(@Param("clientId") Long clientId, @Param("statuses") List<CurrentAccountStatus> statuses);

    @Query("SELECT new org.apache.fineract.currentaccount.data.account.CurrentAccountIdentifiersData(ca.id, ca.accountNumber, ca.externalId) FROM CurrentAccount ca WHERE ca.id = :accountId")
    Optional<CurrentAccountIdentifiersData> findIdentifiersByAccountId(@Param("accountId") String accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({ @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000") })
    @Query("SELECT ca FROM CurrentAccount ca WHERE ca.id = :id")
    Optional<CurrentAccount> findAccountByIdWithExclusiveLock(@Param("id") String id);

    @Query("SELECT 1 FROM CurrentAccount ca WHERE ca.productId = :productId")
    boolean accountsExistsForProduct(@Param("productId") String productId);

    boolean existsByProductId(String productId);
}
