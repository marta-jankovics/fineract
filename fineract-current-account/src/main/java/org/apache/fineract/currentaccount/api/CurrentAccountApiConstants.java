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
    public static final String CURRENT_ACCOUNT_CHARGE_RESOURCE_NAME = "currentaccountcharge";

    // actions
    public static final String submitAction = "submit";
    public static final String cancelAction = "cancel";
    public static final String activateAction = "activate";
    public static final String modifyApplicationAction = "modify";
    public static final String closeAction = "close";

    // command
    public static final String COMMAND = "command";
    public static final String COMMAND_DEPOSIT = "deposit";
    public static final String COMMAND_WITHDRAWAL = "withdrawal";
    public static final String COMMAND_HOLD = "hold";
    public static final String COMMAND_RELEASE = "release";

    // general
    public static final String localeParamName = "locale";
    public static final String dateFormatParamName = "dateFormat";
    public static final String monthDayFormatParamName = "monthDayFormat";
    public static final String staffIdParamName = "currentOfficerId";

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
    public static final String idParamName = "id";
    public static final String isGSIM = "isGSIM";
    public static final String isParentAccount = "isParentAccount";
    public static final String accountNumberParamName = "accountNumber";
    public static final String externalIdParamName = "externalId";
    public static final String statusParamName = "status";
    public static final String reasonForBlockParamName = "reasonForBlock";
    public static final String clientIdParamName = "clientId";
    public static final String isRetailAccountParamName = "isRetailAccount";
    public static final String autogenerateTransactionIdParamName = "autogenerateTransactionId";
    public static final String transactionUpperLimitParamName = "transactionUpperLimit";
    public static final String transactionLowerLimitParamName = "transactionLowerLimit";
    public static final String retailEntriesParamName = "retailEntries";
    public static final String childAccountIdParamName = "childAccountId";
    public static final String interestPostedTillDate = "interestPostedTillDate";

    public static final String groupIdParamName = "groupId";
    public static final String productIdParamName = "productId";
    public static final String fieldOfficerIdParamName = "fieldOfficerId";

    public static final String submittedOnDateParamName = "submittedOnDate";
    public static final String actionDateParamName = "actionDate";
    public static final String identifiersParamName = "identifiers";

    public static final String activeParamName = "active";
    public static final String nameParamName = "name";
    public static final String shortNameParamName = "shortName";
    public static final String descriptionParamName = "description";
    public static final String currencyCodeParamName = "currencyCode";
    public static final String currencyDigitsAfterDecimalParamName = "currencyDigitsAfterDecimal";
    public static final String currencyInMultiplesOfParamName = "currencyInMultiplesOf";
    public static final String nominalAnnualInterestRateParamName = "nominalAnnualInterestRate";
    public static final String interestCompoundingPeriodTypeParamName = "interestCompoundingPeriodType";
    public static final String interestPostingPeriodTypeParamName = "interestPostingPeriodType";
    public static final String interestCalculationTypeParamName = "interestCalculationType";
    public static final String interestCalculationDaysInYearTypeParamName = "interestCalculationDaysInYearType";
    public static final String lockinPeriodFrequencyParamName = "lockinPeriodFrequency";
    public static final String lockinPeriodFrequencyTypeParamName = "lockinPeriodFrequencyType";
    public static final String withdrawalFeeAmountParamName = "withdrawalFeeAmount";
    public static final String withdrawalFeeTypeParamName = "withdrawalFeeType";
    public static final String withdrawalFeeForTransfersParamName = "withdrawalFeeForTransfers";
    public static final String feeAmountParamName = "feeAmount";// to be deleted
    public static final String feeOnMonthDayParamName = "feeOnMonthDay";
    public static final String feeIntervalParamName = "feeInterval";
    public static final String accountingTypeParamName = "accountingType";
    public static final String paymentTypeIdParamName = "paymentTypeId";
    public static final String transactionAccountNumberParamName = "accountNumber";
    public static final String checkNumberParamName = "checkNumber";
    public static final String routingCodeParamName = "routingCode";
    public static final String receiptNumberParamName = "receiptNumber";
    public static final String bankNumberParamName = "bankNumber";
    public static final String enforceParamName = "enforce";
    public static final String allowOverdraftParamName = "allowOverdraft";
    public static final String allowForceTransactionParamName = "allowForceTransaction";
    public static final String balanceCalculationTypeParamName = "balanceCalculationType";
    public static final String overdraftLimitParamName = "overdraftLimit";
    public static final String nominalAnnualInterestRateOverdraftParamName = "nominalAnnualInterestRateOverdraft";
    public static final String minOverdraftForInterestCalculationParamName = "minOverdraftForInterestCalculation";
    public static final String minimumRequiredBalanceParamName = "minimumRequiredBalance";

    public static final String maxAllowedLienLimitParamName = "maxAllowedLienLimit";
    public static final String lienAllowedParamName = "lienAllowed";
    public static final String minBalanceForInterestCalculationParamName = "minBalanceForInterestCalculation";
    public static final String withdrawalBalanceParamName = "withdrawalBalance";
    public static final String onHoldFundsParamName = "onHoldFunds";
    public static final String currentAmountOnHold = "currentAmountOnHold";
    public static final String withHoldTaxParamName = "withHoldTax";
    public static final String taxGroupIdParamName = "taxGroupId";

    // transaction parameters
    public static final String transactionDateParamName = "transactionDate";
    public static final String lienParamName = "lien";
    public static final String transactionAmountParamName = "transactionAmount";
    public static final String paymentDetailDataParamName = "paymentDetailData";
    public static final String runningBalanceParamName = "runningBalance";
    public static final String reversedParamName = "reversed";
    public static final String dateParamName = "date";

    // charges parameters
    public static final String chargeIdParamName = "chargeId";
    public static final String chargesParamName = "charges";
    public static final String currentAccountChargeIdParamName = "currentAccountChargeId";
    public static final String chargeNameParamName = "name";
    public static final String penaltyParamName = "penalty";
    public static final String chargeTimeTypeParamName = "chargeTimeType";
    public static final String dueAsOfDateParamName = "dueDate";
    public static final String chargeCalculationTypeParamName = "chargeCalculationType";
    public static final String percentageParamName = "percentage";
    public static final String amountPercentageAppliedToParamName = "amountPercentageAppliedTo";
    public static final String currencyParamName = "currency";
    public static final String amountWaivedParamName = "amountWaived";
    public static final String amountWrittenOffParamName = "amountWrittenOff";
    public static final String amountOutstandingParamName = "amountOutstanding";
    public static final String amountOrPercentageParamName = "amountOrPercentage";
    public static final String amountParamName = "amount";
    public static final String amountPaidParamName = "amountPaid";
    public static final String chargeOptionsParamName = "chargeOptions";
    public static final String chargePaymentModeParamName = "chargePaymentMode";

    public static final String noteParamName = "note";

    // Current account associations
    public static final String transactions = "transactions";
    public static final String charges = "charges";
    public static final String linkedAccount = "linkedAccount";

    // Current on hold transaction
    public static final String onHoldTransactionTypeParamName = "transactionType";
    public static final String onHoldTransactionDateParamName = "transactionDate";
    public static final String onHoldReversedParamName = "reversed";

    // Current Dormancy
    public static final String isDormancyTrackingActiveParamName = "isDormancyTrackingActive";
    public static final String daysToInactiveParamName = "daysToInactive";
    public static final String daysToDormancyParamName = "daysToDormancy";
    public static final String daysToEscheatParamName = "daysToEscheat";

    public static final String gsimApplicationId = "applicationId";
    public static final String gsimLastApplication = "lastApplication";
    public static final String ERROR_MSG_CURRENT_ACCOUNT_NOT_ACTIVE = "not.in.active.state";

    public static final String accountMappingForPaymentParamName = "accountMappingForPayment";
}
