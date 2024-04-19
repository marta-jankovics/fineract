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
package org.apache.fineract.currentaccount.api.transaction;

import jakarta.ws.rs.core.UriInfo;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionBalanceResponseData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionResponseData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionTemplateResponseData;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.service.PagedLocalRequest;
import org.apache.fineract.portfolio.search.data.AdvancedQueryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CurrentTransactionApi {

    CurrentTransactionTemplateResponseData templateByIdentifier(String accountIdentifier);

    CurrentTransactionTemplateResponseData templateByIdTypeIdentifier(String accountIdType, String accountIdentifier);

    CurrentTransactionTemplateResponseData templateByIdTypeIdentifierSubIdentifier(String accountIdType, String accountIdentifier,
            String accountSubIdentifier);

    Page<CurrentTransactionResponseData> retrieveAllByAccountIdentifier(String accountIdentifier, Pageable pageable);

    Page<CurrentTransactionResponseData> retrieveAllByAccountIdTypeIdentifier(String accountIdType, String accountIdentifier,
            Pageable pageable);

    Page<CurrentTransactionResponseData> retrieveAllByAccountIdTypeIdentifierSubIdentifier(String accountIdType, String accountIdentifier,
            String accountSubIdentifier, Pageable pageable);

    CurrentTransactionResponseData retrieveOneByAccountIdentifierTransactionIdentifier(String accountIdentifier,
            String transactionIdentifier);

    CurrentTransactionResponseData retrieveOneByAccountIdentifierTransactionIdTypeIdentifier(String accountIdentifier,
            String transactionIdType, String transactionIdentifier);

    CurrentTransactionResponseData retrieveOneAccountIdTypeIdentifierTransactionIdentifier(String accountIdType, String accountIdentifier,
            String transactionIdentifier);

    CurrentTransactionResponseData retrieveOneAccountIdTypeIdentifierTransactionIdTypeIdentifier(String accountIdType,
            String accountIdentifier, String transactionIdType, String transactionIdentifier);

    CurrentTransactionResponseData retrieveOneAccountIdTypeIdentifierSubIdentifierTransactionIdentifier(String accountIdType,
            String accountIdentifier, String accountSubIdentifier, String transactionIdentifier);

    CurrentTransactionResponseData retrieveOneAccountIdTypeIdentifierSubIdentifierTransactionIdTypeIdentifier(String accountIdType,
            String accountIdentifier, String accountSubIdentifier, String transactionIdType, String transactionIdentifier);

    CurrentTransactionBalanceResponseData getBalanceByAccountIdentifierTransactionIdentifier(String accountIdentifier,
            String transactionIdentifier);

    CurrentTransactionBalanceResponseData getBalanceByAccountIdentifierTransactionIdTypeIdentifier(String accountIdentifier,
            String transactionIdType, String transactionIdentifier);

    CurrentTransactionBalanceResponseData getBalanceByAccountIdTypeIdentifierTransactionIdentifier(String accountIdType,
            String accountIdentifier, String transactionIdentifier);

    CurrentTransactionBalanceResponseData getBalanceByAccountIdTypeIdentifierTransactionIdTypeIdentifier(String accountIdType,
            String accountIdentifier, String transactionIdType, String transactionIdentifier);

    CurrentTransactionBalanceResponseData getBalanceByAccountIdTypeIdentifierSubIdentifierTransactionIdentifier(String accountIdType,
            String accountIdentifier, String accountSubIdentifier, String transactionIdentifier);

    CurrentTransactionBalanceResponseData getBalanceByAccountIdTypeIdentifierSubIdentifierTransactionIdTypeIdentifier(String accountIdType,
            String accountIdentifier, String accountSubIdentifier, String transactionIdType, String transactionIdentifier);

    CommandProcessingResult transactionByAccountIdentifier(String accountIdentifier, String command, Boolean force, String requestJson);

    CommandProcessingResult transactionByAccountIdTypeIdentifier(String accountIdType, String accountIdentifier, String command,
            Boolean force, String requestJson);

    CommandProcessingResult transactionByAccountIdTypeIdentifierSubIdentifier(String accountIdType, String accountIdentifier,
            String accountSubIdentifier, String command, Boolean force, String requestJson);

    CommandProcessingResult actionByAccountIdentifierTransactionIdentifier(String accountIdentifier, String transactionIdentifier,
            String commandParam, String requestJson);

    CommandProcessingResult actionByAccountIdentifierTransactionIdTypeIdentifier(String accountIdentifier, String transactionIdType,
            String transactionIdentifier, String commandParam, String requestJson);

    CommandProcessingResult actionByAccountIdTypeIdentifierTransactionIdentifier(String accountIdType, String accountIdentifier,
            String transactionIdentifier, String commandParam, String requestJson);

    CommandProcessingResult actionByAccountIdTypeIdentifierTransactionIdTypeIdentifier(String accountIdType, String accountIdentifier,
            String transactionIdType, String transactionIdentifier, String commandParam, String requestJson);

    CommandProcessingResult actionByAccountIdTypeIdentifierSubIdentifierTransactionIdentifier(String accountIdType,
            String accountIdentifier, String accountSubIdentifier, String transactionIdentifier, String commandParam, String requestJson);

    CommandProcessingResult actionByAccountIdTypeIdentifierSubIdentifierTransactionIdTypeIdentifier(String accountIdType,
            String accountIdentifier, String accountSubIdentifier, String transactionIdType, String transactionIdentifier,
            String commandParam, String requestJson);

    String advancedQuery(PagedLocalRequest<AdvancedQueryRequest> queryRequest, UriInfo uriInfo);

    String advancedQueryByAccountIdentifier(String accountIdentifier, PagedLocalRequest<AdvancedQueryRequest> queryRequest,
            UriInfo uriInfo);

    String advancedQueryByAccountIdTypeIdentifier(String accountIdType, String accountIdentifier,
            PagedLocalRequest<AdvancedQueryRequest> queryRequest, UriInfo uriInfo);

    String advancedQueryByAccountIdTypeIdentifierSubIdentifier(String accountIdType, String accountIdentifier, String accountSubIdentifier,
            PagedLocalRequest<AdvancedQueryRequest> queryRequest, UriInfo uriInfo);
}
