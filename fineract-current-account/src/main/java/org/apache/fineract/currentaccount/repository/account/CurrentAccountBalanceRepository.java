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

import jakarta.persistence.LockModeType;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.domain.account.CurrentAccountBalance;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentAccountBalanceRepository extends JpaRepository<CurrentAccountBalance, Long> {

    @Query("SELECT new org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData(cab.id, cab.accountId, cab.accountBalance, "
            + "cab.holdAmount, cab.transactionId, ct.createdDate) FROM CurrentAccountBalance cab "
            + "LEFT JOIN CurrentTransaction ct on ct.id = cab.transactionId WHERE cab.accountId = :accountId")
    CurrentAccountBalanceData getBalanceDataByAccountId(@Param("accountId") String accountId);

    @Query("SELECT ca.id FROM CurrentAccount ca WHERE ca.status in :statuses AND NOT EXISTS (SELECT 1 FROM CurrentAccountBalance cab WHERE cab.accountId = ca.id)")
    List<String> getAccountIdsNoBalance(@Param("statuses") List<CurrentAccountStatus> statuses);

    @Query("SELECT ca.id FROM CurrentAccount ca, CurrentAccountBalance cab WHERE ca.status IN :statuses AND ca.id = cab.accountId AND "
            + " EXISTS (SELECT 1 FROM CurrentTransaction ct WHERE ct.accountId = cab.accountId AND cab.transactionId IS NULL AND ct.createdDate <= :tillDateTime) "
            + " UNION SELECT ca.id FROM CurrentAccount ca, CurrentAccountBalance cab WHERE ca.status IN :statuses AND ca.id = cab.accountId AND "
            + " EXISTS (SELECT 1 FROM CurrentTransaction ct, CurrentTransaction ct2 WHERE ct.accountId = cab.accountId AND ct.accountId = ct2.accountId "
            + " AND cab.transactionId IS NOT NULL AND cab.transactionId = ct2.id AND ct.createdDate > ct2.createdDate AND ct.createdDate <= :tillDateTime) ")
    List<String> getAccountIdsBalanceBehind(@Param("tillDateTime") OffsetDateTime tillDateTime,
            @Param("statuses") List<CurrentAccountStatus> statuses);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select cab from CurrentAccountBalance cab where cab.id = :id")
    Optional<CurrentAccountBalance> findByIdForceIncrement(@Param("id") Long id);
}
