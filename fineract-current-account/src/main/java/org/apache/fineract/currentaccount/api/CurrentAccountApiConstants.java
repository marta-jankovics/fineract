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
package org.apache.fineract.currentaccount.api;

import static org.apache.fineract.infrastructure.configuration.api.ApiConstants.ACTION_CREATE;

import org.apache.fineract.infrastructure.dataqueries.api.DatatableApiConstants;
import org.apache.fineract.statement.data.StatementParser;

@SuppressWarnings({ "HideUtilityClassConstructor" })
public class CurrentAccountApiConstants {

    public static final String CURRENT_PRODUCT_ENTITY_NAME = "CURRENTPRODUCT";
    public static final String CURRENT_ACCOUNT_ENTITY_NAME = "CURRENTACCOUNT";
    public static final String CURRENT_TRANSACTION_ENTITY_NAME = "CURRENTTRANSACTION";
    public static final String CURRENT_IDENTIFIER_ENTITY_NAME = "CURRENTIDENTIFIER";
    public static final String CURRENT_NOTE_ENTITY_NAME = "CURRENTNOTE";
    public static final String CURRENT_TRANSACTION_NOTE_ENTITY_NAME = "CURRENTTRANSACTIONNOTE";

    public static final String CURRENT_PRODUCT_RESOURCE_NAME = CURRENT_PRODUCT_ENTITY_NAME.toLowerCase();
    public static final String CURRENT_ACCOUNT_RESOURCE_NAME = CURRENT_ACCOUNT_ENTITY_NAME.toLowerCase();
    public static final String CURRENT_TRANSACTION_RESOURCE_NAME = CURRENT_TRANSACTION_ENTITY_NAME.toLowerCase();

    // actions
    public static final String CREATE_ACTION = ACTION_CREATE;
    public static final String CANCEL_ACTION = "cancel";
    public static final String ACTIVATE_ACTION = "activate";
    public static final String MODIFY_ACTION = "modify";
    public static final String CLOSE_ACTION = "close";

    // command
    public static final String COMMAND = "command";
    public static final String COMMAND_DEPOSIT = "deposit";
    public static final String COMMAND_WITHDRAWAL = "withdrawal";
    public static final String COMMAND_HOLD = "hold";
    public static final String COMMAND_RELEASE = "release";
    public static final String COMMAND_PARAM_FORCE = "force";

    // general
    public static final String LOCALE_PARAM = "locale";
    public static final String DATE_FORMAT_PARAM = "dateFormat";

    // datatable
    public static final String DATATABLES_PARAM = DatatableApiConstants.DATATABLES_PARAM;
    public static final String DATATABLE_NAME_PARAM = DatatableApiConstants.DATATABLE_NAME_PARAM;
    public static final String DATATABLE_ENTRIES_PARAM = DatatableApiConstants.DATATABLE_ENTRIES_PARAM;
    public static final String DATATABLE_ID_PARAM = DatatableApiConstants.DATATABLE_ID_PARAM;

    // current product and account parameters
    public static final String ACCOUNT_NUMBER_PARAM = "accountNumber";
    public static final String EXTERNAL_ID_PARAM = "externalId";
    public static final String STATUS_PARAM = "status";
    public static final String CLIENT_ID_PARAM = "clientId";
    public static final String PRODUCT_ID_PARAM = "productId";
    public static final String SUBMITTED_ON_DATE_PARAM = "submittedOnDate";
    public static final String ACTION_DATE_PARAM = "actionDate";
    public static final String IDENTIFIERS_PARAM = "identifiers";
    public static final String NAME_PARAM = "name";
    public static final String SHORT_NAME_PARAM = "shortName";
    public static final String DESCRIPTION_PARAM = "description";
    public static final String CURRENCY_CODE_PARAM = "currencyCode";
    public static final String CURRENCY_DIGITS_AFTER_DECIMAL_PARAM = "currencyDigitsAfterDecimal";
    public static final String CURRENCY_IN_MULTIPLES_OF_PARAM = "currencyInMultiplesOf";
    public static final String ACCOUNTING_TYPE_PARAM = "accountingType";
    public static final String PAYMENT_TYPE_ID_PARAM = "paymentTypeId";
    public static final String TRANSACTION_ACCOUNT_NUMBER_PARAM = "accountNumber";
    public static final String ALLOW_OVERDRAFT_PARAM = "allowOverdraft";
    public static final String ALLOW_FORCE_TRANSACTION_PARAM = "allowForceTransaction";
    public static final String BALANCE_CALCULATION_TYPE_PARAM = "balanceCalculationType";
    public static final String OVERDRAFT_LIMIT_PARAM = "overdraftLimit";
    public static final String MINIMUM_REQUIRED_BALANCE_PARAM = "minimumRequiredBalance";
    public static final String STATEMENTS_PARAM = StatementParser.PARAM_STATEMENTS;
    public static final String NOTE_PARAM = "note";
    // Accounting
    public static final String CONTROL_ACCOUNT_ID_PARAM = "controlAccountId";
    public static final String REFERENCE_ACCOUNT_ID_PARAM = "referenceAccountId";
    public static final String OVERDRAFT_CONTROL_ACCOUNT_ID_PARAM = "overdraftControlAccountId";
    public static final String TRANSFERS_IN_SUSPENSE_ACCOUNT_ID_PARAM = "transfersInSuspenseAccountId";
    public static final String WRITE_OFF_ACCOUNT_ID_PARAM = "writeOffAccountId";
    public static final String INCOME_FROM_FEE_ACCOUNT_ID_PARAM = "incomeFromFeeAccountId";
    public static final String INCOME_FROM_PENALTY_ACCOUNT_ID_PARAM = "incomeFromPenaltyAccountId";
    public static final String PAYMENT_CHANNEL_TO_FUND_SOURCE_MAPPINGS_PARAM = "paymentChannelToFundSourceMappings";
    public static final String FUND_SOURCE_ACCOUNT_ID_PARAM = "fundSourceAccountId";
    // Transaction
    public static final String TRANSACTION_DATE_PARAM = "transactionDate";
    public static final String TRANSACTION_AMOUNT_PARAM = "transactionAmount";
    public static final String REASON_FOR_BLOCK_PARAM = "reasonForBlock";
    public static final String TRANSACTION_ID_PARAM = "transactionId";

    // identifier
    public static final String ID_VALUE_PARAM = "value";
    public static final String ID_SUBVALUE_PARAM = "subValue";
    public static final String ID_TYPE_PARAM = "idType";
    public static final String IDENTIFIER_PARAM = "identifier";
    public static final String SUB_IDENTIFIER_PARAM = "subIdentifier";
    public static final String ACCOUNT_ID_TYPE_PARAM = "accountIdType";
    public static final String ACCOUNT_IDENTIFIER_PARAM = "accountIdentifier";
    public static final String ACCOUNT_SUB_IDENTIFIER_PARAM = "accountSubIdentifier";
    public static final String TRANSACTION_ID_TYPE_PARAM = "transactionIdType";
    public static final String TRANSACTION_IDENTIFIER_PARAM = "transactionIdentifier";

    // API
    public static final String SLASH = "/";
    public static final String RESERVED_API_WORDS_REGEX = "(?!transactions|identifiers|statements|notes|query|template)([a-zA-Z_0-9-]+)";
    public static final String RESERVED_API_REGEX = ":" + RESERVED_API_WORDS_REGEX;
    public static final String ID_TYPE_API_REGEX = "{" + ID_TYPE_PARAM + RESERVED_API_REGEX + "}";
    public static final String IDENTIFIER_API_REGEX = "{" + IDENTIFIER_PARAM + RESERVED_API_REGEX + "}";
    public static final String SUB_IDENTIFIER_API_REGEX = "{" + SUB_IDENTIFIER_PARAM + RESERVED_API_REGEX + "}";
    public static final String ACCOUNT_ID_TYPE_API_REGEX = "{" + ACCOUNT_ID_TYPE_PARAM + RESERVED_API_REGEX + "}";
    public static final String ACCOUNT_IDENTIFIER_API_REGEX = "{" + ACCOUNT_IDENTIFIER_PARAM + RESERVED_API_REGEX + "}";
    public static final String ACCOUNT_SUB_IDENTIFIER_API_REGEX = "{" + ACCOUNT_SUB_IDENTIFIER_PARAM + RESERVED_API_REGEX + "}";
    public static final String TRANSACTION_ID_TYPE_API_REGEX = "{" + TRANSACTION_ID_TYPE_PARAM + RESERVED_API_REGEX + "}";
    public static final String TRANSACTION_IDENTIFIER_API_REGEX = "{" + TRANSACTION_IDENTIFIER_PARAM + RESERVED_API_REGEX + "}";

    // search
    public static final String CURRENCY_VIRTUAL_COLUMN = "currency";
    public static final String PAYMENT_TYPE_VIRTUAL_COLUMN = "payment_type";

}
