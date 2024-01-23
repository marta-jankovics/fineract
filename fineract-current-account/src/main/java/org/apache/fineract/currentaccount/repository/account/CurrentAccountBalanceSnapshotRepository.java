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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.domain.account.CurrentAccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentAccountBalanceSnapshotRepository extends JpaRepository<CurrentAccountBalance, UUID> {

    @Query("SELECT new org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData(cabs.id, cabs.accountId, cabs.accountBalance, cabs.holdAmount, ct.createdDate, cabs.calculatedTillTransactionId) FROM CurrentAccountBalance cabs, CurrentTransaction ct WHERE cabs.calculatedTillTransactionId = ct.id AND cabs.accountId = :accountId")
    CurrentAccountBalanceData getBalance(@Param("accountId") UUID accountId);

    @Query("SELECT ca.id FROM CurrentAccount ca, CurrentAccountBalance cabs, (SELECT ct.accountId, MAX(ct.createdDate) as createdDate FROM CurrentTransaction ct WHERE ct.createdDate <= :tillDateTime GROUP BY ct.accountId) lct, CurrentTransaction fct WHERE fct.id = cabs.calculatedTillTransactionId AND ca.id = cabs.accountId AND lct.accountId = ca.id AND lct.createdDate > fct.createdDate")
    List<UUID> getAccountIdsWhereBalanceRecalculationRequired(@Param("tillDateTime") OffsetDateTime tillDateTime);

    Optional<CurrentAccountBalance> findByAccountId(UUID accountId);

    @Query("SELECT ca.id FROM CurrentAccount ca WHERE ca.id NOT IN (SELECT cabs.accountId FROM CurrentAccountBalance cabs)")
    List<UUID> getAccountIdsWhereBalanceSnapshotNotCalculated();
}
