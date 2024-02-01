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
package org.apache.fineract.currentaccount.repository.transaction;

import java.time.OffsetDateTime;
import java.util.List;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentTransactionRepository extends JpaRepository<CurrentTransaction, String> {

    @Query("SELECT new org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData(t.id, t.accountId, t.externalId, t.transactionType, t.transactionDate, t.submittedOnDate, t.amount, t.createdDate, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, curr.name, curr.displaySymbol, pt.id, pt.name, pt.description, pt.isCashPayment, pt.codeName) FROM CurrentTransaction t, ApplicationCurrency curr, CurrentAccount ca, CurrentProduct cp, PaymentType pt WHERE t.accountId = :accountId AND t.id = :transactionId AND ca.id = t.accountId AND ca.productId = ca.id AND curr.code = cp.currency.code AND pt.id = t.paymentTypeId")
    CurrentTransactionData getTransaction(@Param("accountId") String accountId, @Param("transactionId") String transactionId);

    @Query("SELECT new org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData(t.id, t.accountId, t.externalId, t.transactionType, t.transactionDate, t.submittedOnDate, t.amount, t.createdDate, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, curr.name, curr.displaySymbol, pt.id, pt.name, pt.description, pt.isCashPayment, pt.codeName) FROM CurrentTransaction t, ApplicationCurrency curr, CurrentAccount ca, CurrentProduct cp, PaymentType pt WHERE t.accountId = :accountId AND t.externalId = :externalId AND ca.id = t.accountId AND ca.productId = ca.id AND curr.code = cp.currency.code AND pt.id = t.paymentTypeId")
    CurrentTransactionData getTransaction(@Param("accountId") String accountId, @Param("externalId") ExternalId externalId);

    @Query("SELECT new org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData(t.id, t.accountId, t.externalId, t.transactionType, t.transactionDate, t.submittedOnDate, t.amount, t.createdDate, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, curr.name, curr.displaySymbol, pt.id, pt.name, pt.description, pt.isCashPayment, pt.codeName) FROM CurrentTransaction t, ApplicationCurrency curr, CurrentAccount ca, CurrentProduct cp, PaymentType pt WHERE t.accountId = :accountId AND ca.id = t.accountId AND ca.productId = cp.id AND curr.code = cp.currency.code AND pt.id = t.paymentTypeId")
    Page<CurrentTransactionData> getTransactions(@Param("accountId") String accountId, Pageable pageable);

    @Query("SELECT t.id FROM CurrentTransaction t WHERE t.externalId = :externalId")
    String getTransactionId(@Param("externalId") ExternalId externalId);

    @Query("SELECT t FROM CurrentTransaction t, CurrentAccount ca WHERE ca.id = t.accountId AND t.accountId = :accountId")
    List<CurrentTransaction> getTransactions(@Param("accountId") String accountId, Sort sort);

    @Query("SELECT t FROM CurrentTransaction t, CurrentAccount ca WHERE ca.id = t.accountId AND t.accountId = :accountId AND t.createdDate > :fromDateTime ORDER BY t.createdDate, t.id")
    List<CurrentTransaction> getTransactionsFrom(@Param("accountId") String accountId, @Param("fromDateTime") OffsetDateTime fromDateTime);

    @Query("SELECT t FROM CurrentTransaction t, CurrentAccount ca WHERE ca.id = t.accountId AND t.accountId = :accountId AND t.createdDate <= :tillDateTime ORDER By t.createdDate, t.id")
    List<CurrentTransaction> getTransactionsTill(@Param("accountId") String accountId, @Param("tillDateTime") OffsetDateTime tillDateTime);

    @Query("SELECT t FROM CurrentTransaction t, CurrentAccount ca WHERE ca.id = t.accountId AND t.accountId = :accountId AND t.createdDate > :fromDateTime AND t.createdDate <= :tillDateTime ORDER BY t.createdDate, t.id")
    List<CurrentTransaction> getTransactionsFromAndTill(@Param("accountId") String accountId,
            @Param("fromDateTime") OffsetDateTime fromDateTime, @Param("tillDateTime") OffsetDateTime tillDateTime);
}
