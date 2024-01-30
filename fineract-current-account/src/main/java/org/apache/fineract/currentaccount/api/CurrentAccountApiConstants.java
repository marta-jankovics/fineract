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

import org.apache.fineract.infrastructure.dataqueries.api.DatatableApiConstants;

@SuppressWarnings({ "HideUtilityClassConstructor" })
public class CurrentAccountApiConstants {

    public static final String CURRENT_PRODUCT_RESOURCE_NAME = "currentproduct";
    public static final String CURRENT_ACCOUNT_RESOURCE_NAME = "currentaccount";
    public static final String CURRENT_TRANSACTION_RESOURCE_NAME = "currenttransaction";
    public static final String CURRENT_ACCOUNT_TRANSACTION_RESOURCE_NAME = "currentaccount.transaction";

    // actions
    public static final String SUBMIT_ACTION = "submit";
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

    // general
    public static final String LOCALE_PARAM = "locale";
    public static final String DATE_FORMAT_PARAM = "dateFormat";

    // datatable
    public static final String DATATABLES_PARAM = DatatableApiConstants.DATATABLES_PARAM;
    public static final String DATATABLE_NAME_PARAM = DatatableApiConstants.DATATABLE_NAME_PARAM;
    public static final String DATATABLE_ENTRIES_PARAM = DatatableApiConstants.DATATABLE_ENTRIES_PARAM;
    public static final String DATATABLE_ID_PARAM = DatatableApiConstants.DATATABLE_ID_PARAM;

    // query
    public static final String ID_TYPE_PARAM = "idType";
    public static final String IDENTIFIER_PARAM = "identifier";
    public static final String SUB_IDENTIFIER_PARAM = "subIdentifier";

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
    public static final String ENFORCE_PARAM = "enforce";
    public static final String ALLOW_OVERDRAFT_PARAM = "allowOverdraft";
    public static final String ALLOW_FORCE_TRANSACTION_PARAM = "allowForceTransaction";
    public static final String BALANCE_CALCULATION_TYPE_PARAM = "balanceCalculationType";
    public static final String OVERDRAFT_LIMIT_PARAM = "overdraftLimit";
    public static final String MINIMUM_REQUIRED_BALANCE_PARAM = "minimumRequiredBalance";
    public static final String TRANSACTION_DATE_PARAM = "transactionDate";
    public static final String TRANSACTION_AMOUNT_PARAM = "transactionAmount";
    public static final String CONTROL_ACCOUNT_ID_PARAM = "controlAccountId";
    public static final String REFERENCE_ACCOUNT_ID_PARAM = "referenceAccountId";
    public static final String OVERDRAFT_ACCOUNT_ID_PARAM = "overdraftAccountId";
    public static final String TRANSFERS_IN_SUSPENSE_ACCOUNT_ID_PARAM = "transfersInSuspenseAccountId";
    public static final String WRITE_OFF_ACCOUNT_ID_PARAM = "writeOffAccountId";
    public static final String INCOME_FROM_FEE_PARAM = "incomeFromFee";
    public static final String INCOME_FROM_PENALTY_PARAM = "incomeFromPenalty";

    public static final String REASON_FOR_BLOCK_PARAM = "reasonForBlock";

}