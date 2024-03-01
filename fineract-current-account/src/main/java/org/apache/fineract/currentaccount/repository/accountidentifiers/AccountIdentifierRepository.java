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
package org.apache.fineract.currentaccount.repository.accountidentifiers;

import java.util.List;
import java.util.Optional;
import org.apache.fineract.currentaccount.data.account.CurrentAccountIdentifiersData;
import org.apache.fineract.currentaccount.domain.account.AccountIdentifier;
import org.apache.fineract.interoperation.domain.InteropIdentifierType;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountIdentifierRepository extends JpaRepository<AccountIdentifier, Long> {

    @Query("SELECT ai.accountId FROM AccountIdentifier ai WHERE ai.accountType = :accountType AND ai.identifierType = :idType AND ai.value = :id AND ai.subValue = :subId")
    String getAccountIdByIdTypeAndIdentifier(@Param("accountType") PortfolioAccountType accountType,
            @Param("idType") InteropIdentifierType idType, @Param("id") String id, @Param("subId") String subId);

    List<AccountIdentifier> getByAccountTypeAndAccountId(PortfolioAccountType accountType, String accountId);

    AccountIdentifier getByAccountTypeAndAccountIdAndIdentifierType(PortfolioAccountType accountType, String accountId,
            InteropIdentifierType idType);

    @Query("SELECT new org.apache.fineract.currentaccount.data.account.CurrentAccountIdentifiersData(ca.id, ca.accountNumber, ca.externalId) FROM CurrentAccount ca WHERE ca.id = :accountId")
    Optional<CurrentAccountIdentifiersData> findIdentifiersByAccountId(@Param("accountId") String accountId);
}
