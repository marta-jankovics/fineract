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
package org.apache.fineract.currentaccount.repository.accounting;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.fineract.currentaccount.domain.accounting.GLAccountingHistory;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentAccountAccountingRepository extends JpaRepository<GLAccountingHistory, String> {

    Optional<GLAccountingHistory> findByAccountTypeAndAccountId(PortfolioAccountType accountType, String accountId);

    // TODO CURRENT! check not NONE instead of CASH_BASED and validate later if only CASH_BASED is supported
    @Query("select ca.id from CurrentAccount ca, CurrentProduct cp where "
            + "ca.status in :statuses and cp.accountingType = org.apache.fineract.accounting.common.AccountingRuleType.CASH_BASED "
            + "and (not exists (select glah.id from GLAccountingHistory glah where ca.id = glah.accountId) "
            + "or exists (select ct.id from CurrentTransaction ct, CurrentTransaction ct2, GLAccountingHistory glah where ct.accountId = glah.accountId "
            + "and (ca.balanceCalculationType = org.apache.fineract.currentaccount.enumeration.product.BalanceCalculationType.STRICT or ct.createdDate <= :tillDateTime) "
            + "and ct2.id = glah.calculatedTillTransactionId and ct.createdDate > ct2.createdDate))")
    List<String> getAccountIdsForAccounting(@Param("tillDateTime") OffsetDateTime tillDateTime,
            @Param("statuses") List<CurrentAccountStatus> statuses);
}
