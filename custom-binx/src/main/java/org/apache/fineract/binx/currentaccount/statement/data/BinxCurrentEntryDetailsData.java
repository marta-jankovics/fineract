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
package org.apache.fineract.binx.currentaccount.statement.data;

import static org.apache.fineract.binx.BinxConstants.CHANNEL_TYPE_PARTNER_TAXID;
import static org.apache.fineract.binx.BinxConstants.CHANNEL_TYPE_PARTNER_TAXNO;
import static org.apache.fineract.interoperation.domain.InteropIdentifierType.ALIAS;
import static org.apache.fineract.interoperation.domain.InteropIdentifierType.BBAN;
import static org.apache.fineract.interoperation.domain.InteropIdentifierType.IBAN;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import org.apache.fineract.binx.statement.data.BinxTransactionEnvelopeData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData;
import org.apache.fineract.currentaccount.service.account.CurrentAccountResolver;
import org.apache.fineract.interoperation.domain.InteropIdentifierType;
import org.apache.fineract.portfolio.account.domain.AccountIdentifier;
import org.apache.fineract.statement.data.camt053.ContactDetailsData;
import org.apache.fineract.statement.data.camt053.EntryDetailsData;
import org.apache.fineract.statement.data.camt053.OtherContactData;
import org.apache.fineract.statement.data.camt053.PartyData;
import org.apache.fineract.statement.data.camt053.RelatedAccountData;
import org.apache.fineract.statement.data.camt053.RemittanceInfoData;
import org.apache.fineract.statement.data.camt053.SupplementaryData;
import org.apache.fineract.statement.data.camt053.TransactionDetailsData;
import org.apache.fineract.statement.data.camt053.TransactionPartiesData;
import org.apache.fineract.statement.data.camt053.TransactionReferencesData;

@Getter
public class BinxCurrentEntryDetailsData extends EntryDetailsData {

    public static final String DIRECTION_OUT = "OUT";
    public static final String DIRECTION_IN = "IN";

    public BinxCurrentEntryDetailsData(TransactionDetailsData details) {
        super(details);
    }

    public static BinxCurrentEntryDetailsData create(@NotNull CurrentTransactionData transaction,
            @NotNull Map<InteropIdentifierType, AccountIdentifier> identifiers, Map<String, Object> clientDetails, @NotNull String currency,
            Map<String, Object> transactionDetails, String paymentTypeCode) {
        String endToEndId = null;
        String unstructuredInfo = null;
        String transactionIdentification = transaction.getId();
        if (transactionDetails != null) {
            endToEndId = (String) transactionDetails.get("end_to_end_id");
            unstructuredInfo = (String) transactionDetails.get("remittance_information_unstructured");
            transactionIdentification = (String) transactionDetails.get("transaction_id");
        }
        TransactionReferencesData references = TransactionReferencesData.create(endToEndId, transactionIdentification);
        TransactionPartiesData parties = createParties(transaction, identifiers, clientDetails, currency, transactionDetails,
                paymentTypeCode);
        RemittanceInfoData remittanceInfo = RemittanceInfoData.create(unstructuredInfo);
        // create even if other identifiers were added to parties
        SupplementaryData supplementaryData = createSupplementaryData(transaction, identifiers, transactionDetails, paymentTypeCode);
        TransactionDetailsData detailsData = new TransactionDetailsData(references, parties, remittanceInfo, null,
                supplementaryData == null ? null : new SupplementaryData[] { supplementaryData });
        return new BinxCurrentEntryDetailsData(detailsData);
    }

    private static TransactionPartiesData createParties(@NotNull CurrentTransactionData transaction,
            @NotNull Map<InteropIdentifierType, AccountIdentifier> identifiers, Map<String, Object> clientDetails, @NotNull String currency,
            Map<String, Object> transactionDetails, String paymentTypeCode) {
        boolean onUs = paymentTypeCode != null && paymentTypeCode.startsWith("EMY");
        String iban = Optional.ofNullable(identifiers.get(IBAN)).map(AccountIdentifier::getValue)
                .orElse(transactionDetails == null ? null : (String) transactionDetails.get("account_iban"));
        String shortName = clientDetails == null ? null : (String) clientDetails.get("short_name");
        // internal_account_id or bban or id
        final InteropIdentifierType scheme = onUs ? ALIAS : BBAN;
        CurrentAccountResolver identifier = Optional.ofNullable(identifiers.get(scheme))
                .map(e -> CurrentAccountResolver.resolveInternal(scheme, e.getValue(), null))
                .orElse(CurrentAccountResolver.resolveDefault(transaction.getAccountId()));
        PartyData party = PartyData.create(shortName, null, null);
        RelatedAccountData account = RelatedAccountData.create(iban, identifier.getIdentifier(), identifier.getTypeName(), currency);

        PartyData partner = null;
        RelatedAccountData partnerAccount = null;
        boolean outgoing = transaction.getTransactionType().isDebit();
        if (transactionDetails != null) {
            String partnerName = (String) transactionDetails.get("partner_name");
            String partnerIban = (String) transactionDetails.get("partner_account_iban");
            String partnerIdentifier = (String) transactionDetails.get("partner_secondary_identifier");
            String partnerMobile = (String) transactionDetails.get("partner_secondary_id_mobile");
            String partnerEmail = (String) transactionDetails.get("partner_secondary_id_email");
            OtherContactData partnerTaxId = OtherContactData.create(CHANNEL_TYPE_PARTNER_TAXID,
                    (String) transactionDetails.get("partner_secondary_id_tax_id"));
            OtherContactData partnerTaxNo = OtherContactData.create(CHANNEL_TYPE_PARTNER_TAXNO,
                    (String) transactionDetails.get("partner_secondary_id_tax_number"));
            ContactDetailsData contactDetails = ContactDetailsData.create(partnerMobile, partnerEmail, partnerTaxId, partnerTaxNo);
            partner = PartyData.create(partnerName, null, contactDetails);
            partnerAccount = RelatedAccountData.create(partnerIban, partnerIdentifier, null, currency);
            outgoing = DIRECTION_OUT.equalsIgnoreCase((String) transactionDetails.get("direction"));
        }
        if (party == null && account == null && partner == null && partnerAccount == null) {
            return null;
        }
        return outgoing ? new TransactionPartiesData(party, account, partner, partnerAccount)
                : new TransactionPartiesData(partner, partnerAccount, party, account);
    }

    private static SupplementaryData createSupplementaryData(@NotNull CurrentTransactionData transaction,
            @NotNull Map<InteropIdentifierType, AccountIdentifier> identifiers, Map<String, Object> transactionDetails,
            String paymentTypeCode) {
        boolean onUs = paymentTypeCode != null && paymentTypeCode.startsWith("EMY");
        // internal_account_id or bban or id
        final InteropIdentifierType scheme = onUs ? ALIAS : BBAN;
        CurrentAccountResolver identifier = Optional.ofNullable(identifiers.get(scheme))
                .map(e -> CurrentAccountResolver.resolveInternal(scheme, e.getValue(), null))
                .orElse(CurrentAccountResolver.resolveDefault(transaction.getAccountId()));
        String partnerIdentifier = null;
        boolean outgoing = transaction.getTransactionType().isDebit();
        if (transactionDetails != null) {
            partnerIdentifier = (String) transactionDetails.get("partner_secondary_identifier");
            outgoing = DIRECTION_OUT.equalsIgnoreCase((String) transactionDetails.get("direction"));
        }
        BinxTransactionEnvelopeData envelope = outgoing
                ? BinxTransactionEnvelopeData.create(identifier.getIdentifier(), identifier.getTypeName(), partnerIdentifier, null)
                : BinxTransactionEnvelopeData.create(partnerIdentifier, null, identifier.getIdentifier(), identifier.getTypeName());
        return SupplementaryData.create(envelope);
    }
}
