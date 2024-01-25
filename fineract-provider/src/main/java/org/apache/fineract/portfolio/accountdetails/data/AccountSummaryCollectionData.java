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
package org.apache.fineract.portfolio.accountdetails.data;

import org.apache.fineract.currentaccount.data.account.CurrentAccountResponseData;

import java.util.List;

/**
 * Immutable data object representing a summary of various accounts.
 */
@SuppressWarnings("unused")
public class AccountSummaryCollectionData {

    private final List<LoanAccountSummaryData> loanAccounts;
    private final List<LoanAccountSummaryData> groupLoanIndividualMonitoringAccounts;
    private final List<SavingsAccountSummaryData> savingsAccounts;
    private final List<ShareAccountSummaryData> shareAccounts;
    private final List<GuarantorAccountSummaryData> guarantorAccounts;
    private final List<CurrentAccountResponseData> currentAccounts;

    private final List<LoanAccountSummaryData> memberLoanAccounts;
    private final List<SavingsAccountSummaryData> memberSavingsAccounts;
    private final List<GuarantorAccountSummaryData> memberGuarantorAccounts;

    /*
     * METHOD SIGNATURE CHANGE NOTICE: Method's signature was changed for GLIM & GSIM implementation
     */
    public AccountSummaryCollectionData(final List<LoanAccountSummaryData> loanAccounts,
            final List<LoanAccountSummaryData> groupLoanIndividualMonitoringAccounts,
            final List<SavingsAccountSummaryData> savingsAccounts, final List<ShareAccountSummaryData> shareAccounts,
            final List<GuarantorAccountSummaryData> guarantorAccounts, final List<CurrentAccountResponseData> currentAccounts) {

        this.loanAccounts = defaultIfEmpty(loanAccounts);
        this.groupLoanIndividualMonitoringAccounts = defaultIfEmpty(groupLoanIndividualMonitoringAccounts);
        this.savingsAccounts = defaultIfEmpty(savingsAccounts);
        this.shareAccounts = defaultIfEmpty(shareAccounts);
        this.guarantorAccounts = defaultIfEmpty(guarantorAccounts);
        this.currentAccounts = defaultIfEmpty(currentAccounts);
        this.memberLoanAccounts = null;
        this.memberSavingsAccounts = null;
        this.memberGuarantorAccounts = null;
    }

    public AccountSummaryCollectionData(final List<LoanAccountSummaryData> loanAccounts,
            final List<LoanAccountSummaryData> groupLoanIndividualMonitoringAccounts,
            final List<SavingsAccountSummaryData> savingsAccounts, final List<GuarantorAccountSummaryData> guarantorAccounts,
            final List<LoanAccountSummaryData> memberLoanAccounts, final List<SavingsAccountSummaryData> memberSavingsAccounts,
            final List<GuarantorAccountSummaryData> memberGuarantorAccounts) {
        /* Note to Self: GSIM not passed in */

        this.loanAccounts = defaultIfEmpty(loanAccounts);
        this.groupLoanIndividualMonitoringAccounts = defaultIfEmpty(groupLoanIndividualMonitoringAccounts);
        this.savingsAccounts = defaultIfEmpty(savingsAccounts);
        this.guarantorAccounts = defaultIfEmpty(guarantorAccounts);
        this.shareAccounts = null;
        this.currentAccounts = null;
        this.memberLoanAccounts = defaultIfEmpty(memberLoanAccounts);
        this.memberSavingsAccounts = defaultIfEmpty(memberSavingsAccounts);
        this.memberGuarantorAccounts = defaultIfEmpty(memberGuarantorAccounts);
    }

    private <T> List<T> defaultIfEmpty(final List<T> List) {
        List<T> returnList = null;
        if (List != null && !List.isEmpty()) {
            returnList = List;
        }
        return returnList;
    }
}
