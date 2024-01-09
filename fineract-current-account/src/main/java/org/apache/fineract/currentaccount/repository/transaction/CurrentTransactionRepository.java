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
import java.util.UUID;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentTransactionRepository extends JpaRepository<CurrentTransaction, UUID> {

    @Query("SELECT new org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData(t.id, t.accountId, t.externalId, t.transactionType, t.transactionDate, t.submittedOnDate, t.transactionAmount, t.createdDate, curr.code, curr.name, curr.nameCode, curr.displaySymbol, curr.decimalPlaces, curr.inMultiplesOf, pd.id, pd.accountNumber, pd.checkNumber, pd.routingCode, pd.receiptNumber, pd.bankNumber, pt.id, pt.name, pt.description, pt.isCashPayment, pt.position, pt.codeName, pt.isSystemDefined) FROM CurrentTransaction t, ApplicationCurrency curr, CurrentAccount ca, PaymentDetail pd, PaymentType pt WHERE t.accountId = :accountId AND t.id = :transactionId AND ca.id = t.accountId and curr.code = ca.currency.code and t.paymentDetailId = pd.id and pd.paymentType.id = pt.id")
    CurrentTransactionData findByIdAndAccountId(@Param("accountId") UUID accountId, @Param("transactionId") UUID transactionId);

    @Query("SELECT new org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData(t.id, t.accountId, t.externalId, t.transactionType, t.transactionDate, t.submittedOnDate, t.transactionAmount, t.createdDate, curr.code, curr.name, curr.nameCode, curr.displaySymbol, curr.decimalPlaces, curr.inMultiplesOf, pd.id, pd.accountNumber, pd.checkNumber, pd.routingCode, pd.receiptNumber, pd.bankNumber, pt.id, pt.name, pt.description, pt.isCashPayment, pt.position, pt.codeName, pt.isSystemDefined) FROM CurrentTransaction t, ApplicationCurrency curr, CurrentAccount ca, PaymentDetail pd, PaymentType pt WHERE t.accountId = :accountId AND ca.id = t.accountId and curr.code = ca.currency.code and t.paymentDetailId = pd.id and pd.paymentType.id = pt.id")
    Page<CurrentTransactionData> findByAccountId(@Param("accountId") UUID accountId, Pageable pageable);

    @Query("SELECT new org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData(t.id, t.accountId, t.externalId, t.transactionType, t.transactionDate, t.submittedOnDate, t.transactionAmount, t.createdDate) FROM CurrentTransaction t, CurrentAccount ca WHERE ca.id = t.accountId AND t.accountId = :accountId AND t.createdDate > :from")
    List<CurrentTransactionData> getTransactionsFrom(@Param("accountId") UUID accountId, @Param("from") OffsetDateTime from);

    @Query("SELECT new org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData(t.id, t.accountId, t.externalId, t.transactionType, t.transactionDate, t.submittedOnDate, t.transactionAmount, t.createdDate) FROM CurrentTransaction t, CurrentAccount ca WHERE ca.id = t.accountId AND t.accountId = :accountId")
    List<CurrentTransactionData> getTransactions(@Param("accountId") UUID accountId);
}
