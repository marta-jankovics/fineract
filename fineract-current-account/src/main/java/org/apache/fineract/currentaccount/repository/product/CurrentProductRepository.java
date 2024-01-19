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
package org.apache.fineract.currentaccount.repository.product;

import java.util.List;
import java.util.UUID;
import org.apache.fineract.currentaccount.data.product.CurrentProductData;
import org.apache.fineract.currentaccount.domain.product.CurrentProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentProductRepository extends JpaRepository<CurrentProduct, UUID> {

    @Query("SELECT new org.apache.fineract.currentaccount.data.product.CurrentProductData(cp.id, cp.name, cp.shortName, cp.description,  cp.accountingType, cp.allowOverdraft, cp.overdraftLimit, cp.minRequiredBalance, cp.allowForceTransaction, cp.balanceCalculationType, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, curr.name, curr.displaySymbol) FROM CurrentProduct cp, ApplicationCurrency curr WHERE curr.code = cp.currency.code ")
    Page<CurrentProductData> findAllCurrentProductData(Pageable pageable);

    @Query("SELECT new org.apache.fineract.currentaccount.data.product.CurrentProductData(cp.id, cp.name, cp.shortName, cp.description, cp.accountingType, cp.allowOverdraft, cp.overdraftLimit,  cp.minRequiredBalance, cp.allowForceTransaction, cp.balanceCalculationType, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, curr.name, curr.displaySymbol) FROM CurrentProduct cp, ApplicationCurrency curr WHERE curr.code = cp.currency.code  AND cp.id = :productId")
    CurrentProductData findCurrentProductData(@Param("productId") UUID productId);

    @Query("SELECT new org.apache.fineract.currentaccount.data.product.CurrentProductData(cp.id, cp.name, cp.shortName, cp.description, cp.accountingType, cp.allowOverdraft, cp.overdraftLimit, cp.minRequiredBalance, cp.allowForceTransaction, cp.balanceCalculationType, ./gracp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, curr.name, curr.displaySymbol) FROM CurrentProduct cp, ApplicationCurrency curr WHERE curr.code = cp.currency.code ")
    List<CurrentProductData> findAllSorted(Sort sort);
}
