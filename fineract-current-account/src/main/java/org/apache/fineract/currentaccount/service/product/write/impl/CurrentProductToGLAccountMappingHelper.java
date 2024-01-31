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
package org.apache.fineract.currentaccount.service.product.write.impl;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CONTROL_ACCOUNT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.FUND_SOURCE_ACCOUNT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.INCOME_FROM_FEE_ACCOUNT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.INCOME_FROM_PENALTY_ACCOUNT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.OVERDRAFT_CONTROL_ACCOUNT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.PAYMENT_CHANNEL_TO_FUND_SOURCE_MAPPINGS_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.PAYMENT_TYPE_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.REFERENCE_ACCOUNT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.TRANSFERS_IN_SUSPENSE_ACCOUNT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.WRITE_OFF_ACCOUNT_ID_PARAM;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.accounting.common.AccountingConstants;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.accounting.producttoaccountmapping.domain.ProductToGLAccountMapping;
import org.apache.fineract.accounting.producttoaccountmapping.domain.ProductToGLAccountMappingRepository;
import org.apache.fineract.accounting.producttoaccountmapping.exception.ProductToGLAccountMappingNotFoundException;
import org.apache.fineract.accounting.producttoaccountmapping.service.ProductToGLAccountMappingHelper;
import org.apache.fineract.currentaccount.domain.product.CurrentProduct;
import org.apache.fineract.currentaccount.enumeration.product.CurrentProductCashAccounts;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.paymenttype.domain.PaymentType;
import org.apache.fineract.portfolio.paymenttype.domain.PaymentTypeRepositoryWrapper;

@Slf4j
@RequiredArgsConstructor
public class CurrentProductToGLAccountMappingHelper {

    private final ProductToGLAccountMappingRepository accountMappingRepository;
    private final ProductToGLAccountMappingHelper productToGLAccountMappingHelper;
    private final PaymentTypeRepositoryWrapper paymentTypeRepositoryWrapper;

    public void createCurrentProductAccountMapping(final JsonCommand command, final String paramName, final CurrentProduct product,
            final CurrentProductCashAccounts accountType) {
        final Long accountId = command.longValueOfParameterNamed(paramName);
        final GLAccount glAccount = productToGLAccountMappingHelper.getAccountByIdAndType(paramName, accountType.getType(), accountId);
        final ProductToGLAccountMapping accountMapping = new ProductToGLAccountMapping().setGlAccount(glAccount)
                .setProductIdentifier(product.getId()).setProductType(PortfolioProductType.CURRENT.getValue())
                .setFinancialAccountType(accountType.getId());
        this.accountMappingRepository.save(accountMapping);
    }

    public void updateCurrentProductAccountMappingChanges(final JsonCommand command, final String paramName, final CurrentProduct product,
            CurrentProductCashAccounts cashAccounts, final Map<String, Object> changes) {
        final Long accountId = command.longValueOfParameterNamed(paramName);

        final ProductToGLAccountMapping accountMapping = this.accountMappingRepository.findCoreProductToFinAccountMapping(product.getId(),
                PortfolioProductType.CURRENT.getValue(), cashAccounts.getId());
        if (accountMapping == null) {
            throw new ProductToGLAccountMappingNotFoundException(PortfolioProductType.CURRENT, product.getId(), cashAccounts.name());
        } else {
            if (accountMapping.getGlAccount() != null && !Objects.equals(accountMapping.getGlAccount().getId(), accountId)) {
                final GLAccount glAccount = productToGLAccountMappingHelper.getAccountByIdAndType(paramName, cashAccounts.getType(),
                        accountId);
                changes.put(paramName, accountId);
                accountMapping.setGlAccount(glAccount);
            }
        }

    }

    public void savePaymentChannelToFundSourceMappings(final JsonCommand command, CurrentProduct currentProduct,
            final Map<String, Object> changes) {
        final JsonArray paymentChannelMappingArray = command.arrayOfParameterNamed(PAYMENT_CHANNEL_TO_FUND_SOURCE_MAPPINGS_PARAM);
        if (paymentChannelMappingArray != null) {
            if (changes != null) {
                changes.put(PAYMENT_CHANNEL_TO_FUND_SOURCE_MAPPINGS_PARAM,
                        command.jsonFragment(PAYMENT_CHANNEL_TO_FUND_SOURCE_MAPPINGS_PARAM));
            }
            for (int i = 0; i < paymentChannelMappingArray.size(); i++) {
                final JsonObject jsonObject = paymentChannelMappingArray.get(i).getAsJsonObject();
                final Long paymentTypeId = jsonObject.get(AccountingConstants.LoanProductAccountingParams.PAYMENT_TYPE.getValue())
                        .getAsLong();
                final Long paymentSpecificFundAccountId = jsonObject
                        .get(AccountingConstants.LoanProductAccountingParams.FUND_SOURCE.getValue()).getAsLong();
                savePaymentChannelToFundSourceMapping(currentProduct, paymentTypeId, paymentSpecificFundAccountId);
            }
        }
    }

    public void updatePaymentChannelToFundSourceMappings(final JsonCommand command, final CurrentProduct product,
            final Map<String, Object> changes) {
        // find all existing payment Channel to Fund source Mappings
        final List<ProductToGLAccountMapping> existingPaymentChannelToFundSourceMappings = this.accountMappingRepository
                .findAllPaymentTypeToFundSourceMappings(product.getId(), PortfolioProductType.CURRENT.getValue());
        final JsonArray paymentChannelMappingArray = command.arrayOfParameterNamed(PAYMENT_CHANNEL_TO_FUND_SOURCE_MAPPINGS_PARAM);
        /**
         * Variable stores a map representation of Payment channels (key) and their fund sources (value) extracted from
         * the passed in Jsoncommand
         **/
        final Map<Long, Long> inputPaymentChannelFundSourceMap = new HashMap<>();
        /***
         * Variable stores all payment types which have already been mapped to Fund Sources in the system
         **/
        final Set<Long> existingPaymentTypes = new HashSet<>();
        if (paymentChannelMappingArray != null) {
            if (changes != null) {
                changes.put(PAYMENT_CHANNEL_TO_FUND_SOURCE_MAPPINGS_PARAM,
                        command.jsonFragment(PAYMENT_CHANNEL_TO_FUND_SOURCE_MAPPINGS_PARAM));
            }

            for (int i = 0; i < paymentChannelMappingArray.size(); i++) {
                final JsonObject jsonObject = paymentChannelMappingArray.get(i).getAsJsonObject();
                final Long paymentTypeId = jsonObject.get(PAYMENT_TYPE_ID_PARAM).getAsLong();
                final Long paymentSpecificFundAccountId = jsonObject.get(FUND_SOURCE_ACCOUNT_ID_PARAM).getAsLong();
                inputPaymentChannelFundSourceMap.put(paymentTypeId, paymentSpecificFundAccountId);
            }

            // If input map is empty, delete all existing mappings
            if (inputPaymentChannelFundSourceMap.isEmpty()) {
                this.accountMappingRepository.deleteAllInBatch(existingPaymentChannelToFundSourceMappings);
            } else {
                for (final ProductToGLAccountMapping existingPaymentChannelToFundSourceMapping : existingPaymentChannelToFundSourceMappings) {
                    final Long currentPaymentChannelId = existingPaymentChannelToFundSourceMapping.getPaymentType().getId();
                    existingPaymentTypes.add(currentPaymentChannelId);
                    // update existing mappings (if required)
                    if (inputPaymentChannelFundSourceMap.containsKey(currentPaymentChannelId)) {
                        final Long newGLAccountId = inputPaymentChannelFundSourceMap.get(currentPaymentChannelId);
                        if (!newGLAccountId.equals(existingPaymentChannelToFundSourceMapping.getGlAccount().getId())) {
                            final GLAccount glAccount = productToGLAccountMappingHelper.getAccountById(FUND_SOURCE_ACCOUNT_ID_PARAM,
                                    newGLAccountId);
                            existingPaymentChannelToFundSourceMapping.setGlAccount(glAccount);
                        }
                    } // deleted payment type
                    else {
                        this.accountMappingRepository.delete(existingPaymentChannelToFundSourceMapping);
                    }
                }

                // only the newly added
                for (Map.Entry<Long, Long> entry : inputPaymentChannelFundSourceMap.entrySet().stream()
                        .filter(e -> !existingPaymentTypes.contains(e.getKey())).toList()) {
                    savePaymentChannelToFundSourceMapping(product, entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public void handleChangesToCurrentProductProductToGLAccountMappings(final CurrentProduct product, final JsonCommand command,
            final Map<String, Object> changes) {

        // asset
        updateCurrentProductAccountMappingChanges(command, REFERENCE_ACCOUNT_ID_PARAM, product, CurrentProductCashAccounts.REFERENCE,
                changes);

        updateCurrentProductAccountMappingChanges(command, OVERDRAFT_CONTROL_ACCOUNT_ID_PARAM, product,
                CurrentProductCashAccounts.OVERDRAFT_CONTROL, changes);

        // income
        updateCurrentProductAccountMappingChanges(command, INCOME_FROM_FEE_ACCOUNT_ID_PARAM, product,
                CurrentProductCashAccounts.INCOME_FROM_FEES, changes);

        updateCurrentProductAccountMappingChanges(command, INCOME_FROM_PENALTY_ACCOUNT_ID_PARAM, product,
                CurrentProductCashAccounts.INCOME_FROM_PENALTIES, changes);

        // expenses
        updateCurrentProductAccountMappingChanges(command, WRITE_OFF_ACCOUNT_ID_PARAM, product,
                CurrentProductCashAccounts.LOSSES_WRITTEN_OFF, changes);

        // liability
        updateCurrentProductAccountMappingChanges(command, CONTROL_ACCOUNT_ID_PARAM, product, CurrentProductCashAccounts.CONTROL, changes);
        updateCurrentProductAccountMappingChanges(command, TRANSFERS_IN_SUSPENSE_ACCOUNT_ID_PARAM, product,
                CurrentProductCashAccounts.TRANSFERS_SUSPENSE, changes);

    }

    public void deleteProductToGLAccountMapping(final CurrentProduct product) {
        final List<ProductToGLAccountMapping> productToGLAccountMappings = this.accountMappingRepository
                .findByProductIdentifierAndProductType(product.getId(), PortfolioProductType.CURRENT.getValue());
        if (!productToGLAccountMappings.isEmpty()) {
            this.accountMappingRepository.deleteAllInBatch(productToGLAccountMappings);
        }
    }

    private void savePaymentChannelToFundSourceMapping(final CurrentProduct product, final Long paymentTypeId,
            final Long paymentTypeSpecificFundAccountId) {
        final PaymentType paymentType = this.paymentTypeRepositoryWrapper.findOneWithNotFoundDetection(paymentTypeId);
        final GLAccount glAccount = productToGLAccountMappingHelper
                .getAccountById(AccountingConstants.LoanProductAccountingParams.FUND_SOURCE.getValue(), paymentTypeSpecificFundAccountId);
        final ProductToGLAccountMapping accountMapping = new ProductToGLAccountMapping().setGlAccount(glAccount)
                .setProductIdentifier(product.getId()).setProductType(PortfolioProductType.CURRENT.getValue())
                .setFinancialAccountType(AccountingConstants.CashAccountsForLoan.FUND_SOURCE.getValue()).setPaymentType(paymentType);
        this.accountMappingRepository.save(accountMapping);
    }
}
