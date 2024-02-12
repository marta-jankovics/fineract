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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.apache.fineract.currentaccount.domain.account.CurrentAccountBalance;
import org.apache.fineract.currentaccount.domain.account.CurrentAccountDailyBalance;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentAccountDailyBalanceRepository extends JpaRepository<CurrentAccountDailyBalance, String> {

    Optional<CurrentAccountBalance> findByAccountId(String accountId);

    @Query("select cadb from CurrentAccountDailyBalance cadb, "
            + "(select max(cadb2.balanceDate) as maxDate from CurrentAccountDailyBalance cadb2 where cadb2.accountId = :accountId and cadb2.balanceDate < :date) maxdb "
            + "where cadb.accountId = :accountId and cadb.balanceDate = maxdb.maxDate")
    CurrentAccountDailyBalance getLatestDailyBalanceBefore(@Param("accountId") String accountId, @Param("date") LocalDate date);

    @Query("select ca.id from CurrentAccount ca where ca.status in :statuses "
            + "and not exists (select cadb.id from CurrentAccountDailyBalance cadb where cadb.accountId = ca.id and cadb.balanceDate = :date)")
    List<String> getAccountIdsForDailyBalanceCalculation(@Param("date") LocalDate date,
            @Param("statuses") List<CurrentAccountStatus> statuses);
}
