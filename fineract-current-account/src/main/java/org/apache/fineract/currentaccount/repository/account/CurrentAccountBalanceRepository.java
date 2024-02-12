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
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.domain.account.CurrentAccountBalance;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentAccountBalanceRepository extends JpaRepository<CurrentAccountBalance, Long> {

    Optional<CurrentAccountBalance> findByAccountId(String accountId);

    @Query("SELECT new org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData(cab.id, cab.accountId, cab.accountBalance, "
            + "cab.holdAmount, cab.transactionId, ct.createdDate) FROM CurrentAccountBalance cab, CurrentTransaction ct "
            + "WHERE cab.transactionId = ct.id AND cab.accountId = :accountId")
    CurrentAccountBalanceData getBalanceDataByAccountId(@Param("accountId") String accountId);

    @Query("select ca.id from CurrentAccount ca where ca.status in :statuses and not exists (select cab.id from CurrentAccountBalance cab where cab.accountId = ca.id)")
    List<String> getAccountIdsNoBalance(@Param("statuses") List<CurrentAccountStatus> statuses);

    @Query("select ca.id from CurrentAccount ca where ca.status in :statuses and "
            + "exists (select ct.id from CurrentTransaction ct, CurrentTransaction ct2, CurrentAccountBalance cab where ct.accountId = ca.id and ct.accountId = cab.accountId and "
            + "(ca.balanceCalculationType = org.apache.fineract.currentaccount.enumeration.product.BalanceCalculationType.STRICT or ct.createdDate <= :tillDateTime) and "
            + "ct2.id = cab.transactionId and ct.createdDate > ct2.createdDate)")
    List<String> getAccountIdsBalanceBehind(@Param("tillDateTime") OffsetDateTime tillDateTime,
            @Param("statuses") List<CurrentAccountStatus> statuses);
}