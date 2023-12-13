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
package org.apache.fineract.portfolio.savings.statement.data;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Getter;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.statement.data.camt053.BalanceAmountData;
import org.apache.fineract.portfolio.statement.data.camt053.EntryDetailsData;
import org.apache.fineract.portfolio.statement.data.camt053.PartyData;
import org.apache.fineract.portfolio.statement.data.camt053.RelatedAccountData;
import org.apache.fineract.portfolio.statement.data.camt053.RemittanceInfoData;
import org.apache.fineract.portfolio.statement.data.camt053.SupplementaryData;
import org.apache.fineract.portfolio.statement.data.camt053.TransactionDetailsData;
import org.apache.fineract.portfolio.statement.data.camt053.TransactionPartiesData;
import org.apache.fineract.portfolio.statement.data.camt053.TransactionReferencesData;

@Getter
public class SavingsEntryDetailsData extends EntryDetailsData {

    public SavingsEntryDetailsData(TransactionDetailsData details) {
        super(details == null ? null : new TransactionDetailsData[] { details });
    }

    public static SavingsEntryDetailsData create(@NotNull SavingsAccountTransaction transaction, Map<String, Object> clientDetails,
            @NotNull String currency, @NotNull Map<String, Object> details) {
        String endToEndId = (String) details.get("end_to_end_id");
        TransactionReferencesData references = TransactionReferencesData.create(endToEndId, String.valueOf(transaction.getId()));
        BalanceAmountData balance = new BalanceAmountData(transaction.getAmount(), currency);
        String paymentTypeCode = (String) details.get("payment_type_code");
        if (paymentTypeCode == null) {
            PaymentDetail paymentDetail = transaction.getPaymentDetail();
            paymentTypeCode = paymentDetail == null ? null : paymentDetail.getPaymentType().getName();
        }
        TransactionPartiesData parties = createParties(clientDetails, currency, details, paymentTypeCode);
        String unstructuredInfo = (String) details.get("remittance_information_unstructured");
        RemittanceInfoData remittanceInfo = RemittanceInfoData.create(unstructuredInfo);
        // create even if other identifiers were added to parties
        SupplementaryData supplementaryData = createSupplementaryData(clientDetails, details, paymentTypeCode);
        TransactionDetailsData detailsData = new TransactionDetailsData(references, balance, parties, remittanceInfo, paymentTypeCode,
                supplementaryData == null ? null : new SupplementaryData[] { supplementaryData });
        return new SavingsEntryDetailsData(detailsData);
    }

    private static TransactionPartiesData createParties(Map<String, Object> clientDetails, @NotNull String currency,
            Map<String, Object> details, String paymentTypeCode) {
        boolean onUs = paymentTypeCode != null && paymentTypeCode.startsWith("EMY");
        String iban = (String) details.get("account_iban");
        String shortName = null;
        String identifier = null;
        if (clientDetails != null) {
            shortName = (String) clientDetails.get("short_name");
            identifier = onUs ? (String) clientDetails.get("internal_account_id") : (String) clientDetails.get("bban");
        }
        String partnerName = (String) details.get("partner_name");
        String partnerIban = (String) details.get("partner_account_iban");
        String partnerIdentifier = (String) details.get("partner_secondary_identifier");

        PartyData party = PartyData.create(shortName, null);
        RelatedAccountData account = RelatedAccountData.create(iban, identifier, currency);
        PartyData partner = PartyData.create(partnerName, null);
        RelatedAccountData partnerAccount = RelatedAccountData.create(partnerIban, partnerIdentifier, currency);
        if (party == null && account == null && partner == null && partnerAccount == null) {
            return null;
        }

        boolean outgoing = (Boolean) details.get("isOutgoing");
        return outgoing ? new TransactionPartiesData(party, account, partner, partnerAccount)
                : new TransactionPartiesData(partner, partnerAccount, party, account);
    }

    private static SupplementaryData createSupplementaryData(Map<String, Object> clientDetails, @NotNull Map<String, Object> details,
            String paymentTypeCode) {
        boolean onUs = paymentTypeCode != null && paymentTypeCode.startsWith("EMY");
        String identifier = null;
        if (clientDetails != null) {
            identifier = onUs ? (String) clientDetails.get("internal_account_id") : (String) clientDetails.get("bban");
        }
        String partnerIdentifier = (String) details.get("partner_secondary_identifier");
        boolean outgoing = (Boolean) details.get("isOutgoing");
        SavingsTransactionEnvelopeData envelope = outgoing ? SavingsTransactionEnvelopeData.create(identifier, partnerIdentifier)
                : SavingsTransactionEnvelopeData.create(partnerIdentifier, identifier);
        return SupplementaryData.create(envelope);
    }
}
