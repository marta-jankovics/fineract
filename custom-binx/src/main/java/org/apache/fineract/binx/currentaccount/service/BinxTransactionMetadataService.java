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
package org.apache.fineract.binx.currentaccount.service;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.domain.transaction.ICurrentTransaction;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.transaction.write.impl.CurrentTransactionMetadataServiceImpl;
import org.apache.fineract.portfolio.paymenttype.domain.PaymentTypeRepository;
import org.apache.fineract.portfolio.transaction.domain.TransactionParamRepository;
import org.apache.logging.log4j.util.Strings;

@Slf4j
public class BinxTransactionMetadataService extends CurrentTransactionMetadataServiceImpl {

    private final BinxCurrentDetailsReadService detailsReadService;
    private final PaymentTypeRepository paymentTypeRepository;

    public BinxTransactionMetadataService(CurrentAccountRepository accountRepository, CurrentTransactionRepository transactionRepository,
            TransactionParamRepository transactionParamRepository, BinxCurrentDetailsReadService detailsReadService,
            PaymentTypeRepository paymentTypeRepository) {
        super(accountRepository, transactionRepository, transactionParamRepository);
        this.detailsReadService = detailsReadService;
        this.paymentTypeRepository = paymentTypeRepository;
    }

    @NotNull
    @Override
    protected String calculateTransactionName(@NotNull CurrentAccountData account, @NotNull ICurrentTransaction transaction, int sequence) {
        String code = null;
        Long paymentTypeId = transaction.getPaymentTypeId();
        if (paymentTypeId != null) {
            String paymentTypeCode = paymentTypeRepository.getNameById(paymentTypeId);
            if (paymentTypeCode != null) {
                List<Map<String, Object>> metadata = detailsReadService.getTransactionMetadataDetails(account.getOfficeId());
                if (!metadata.isEmpty()) {
                    String dtCode = metadata.stream().filter(e -> paymentTypeCode.equalsIgnoreCase((String) e.get("payment_type_code")))
                            .findFirst().map(e -> (String) e.get("metadata_code")).orElse(null);
                    if (!Strings.isEmpty(dtCode)) {
                        code = dtCode;
                    }
                }
                if (code == null) {
                    code = paymentTypeCode;
                    code = code.replace(" ", "");
                    code = code.substring(0, Math.min(code.length(), 20));
                }
            }
        }
        if (code == null) {
            code = transaction.getId().substring(0, 8);
        }
        return code + METADATA_DATE_FORMATTER.format(transaction.getSubmittedOnDate()) + String.format("%06d", sequence);
    }
}
