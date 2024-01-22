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

import java.util.UUID;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentAccountRepository extends JpaRepository<CurrentAccount, UUID> {

    @Query("SELECT new org.apache.fineract.currentaccount.data.account.CurrentAccountData(ca.id, ca.accountNo, ca.externalId, ca.clientId, ca.productId, ca.status, ca.activatedOnDate, ca.allowOverdraft, ca.overdraftLimit, ca.allowForceTransaction, ca.minimumRequiredBalance, ca.balanceCalculationType, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, curr.name, curr.displaySymbol) FROM CurrentAccount ca, CurrentProduct cp, ApplicationCurrency curr WHERE ca.productId = cp.id AND curr.code = cp.currency.code AND ca.id = :id")
    CurrentAccountData findCurrentAccountData(@Param("id") UUID id);

    @Query("SELECT new org.apache.fineract.currentaccount.data.account.CurrentAccountData(ca.id, ca.accountNo, ca.externalId, ca.clientId, ca.productId, ca.status, ca.activatedOnDate, ca.allowOverdraft, ca.overdraftLimit, ca.allowForceTransaction, ca.minimumRequiredBalance, ca.balanceCalculationType, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, curr.name, curr.displaySymbol) FROM CurrentAccount ca, CurrentProduct cp, ApplicationCurrency curr WHERE ca.productId = cp.id AND curr.code = cp.currency.code  AND ca.externalId = :externalId")
    CurrentAccountData findCurrentAccountData(@Param("externalId") ExternalId externalId);

    @Query("SELECT new org.apache.fineract.currentaccount.data.account.CurrentAccountData(ca.id, ca.accountNo, ca.externalId, ca.clientId, ca.productId, ca.status, ca.activatedOnDate, ca.allowOverdraft, ca.overdraftLimit, ca.allowForceTransaction, ca.minimumRequiredBalance, ca.balanceCalculationType, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, curr.name, curr.displaySymbol) FROM CurrentAccount ca, CurrentProduct cp, ApplicationCurrency curr WHERE ca.productId = cp.id AND curr.code = cp.currency.code")
    Page<CurrentAccountData> findAllCurrentAccountData(Pageable pageable);

    UUID findIdByExternalId(ExternalId accountExternalId);
}
