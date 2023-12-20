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
package org.apache.fineract.currentaccount.repository;

import org.apache.fineract.currentaccount.data.CurrentProductData;
import org.apache.fineract.currentaccount.domain.CurrentProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentProductRepository extends JpaRepository<CurrentProduct, Long> {

    String FIND_CURRENT_PRODUCT_DETAILS = "SELECT new org.apache.fineract.currentaccount.data.CurrentProductData(cp.id, cp.name, cp.shortName, cp.description, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, cp.accountingType, cp.allowOverdraft, cp.overdraftLimit, cp.minRequiredBalance, curr.name, curr.nameCode, curr.displaySymbol) FROM CurrentProduct cp, ApplicationCurrency curr WHERE curr.code = cp.currency.code ";

    @Query(FIND_CURRENT_PRODUCT_DETAILS)
    Page<CurrentProductData> findAllCurrentProductData(Pageable pageable);

    @Query(FIND_CURRENT_PRODUCT_DETAILS + " AND cp.id = :productId")
    CurrentProductData findCurrentProductData(@Param("productId") Long productId);
}
