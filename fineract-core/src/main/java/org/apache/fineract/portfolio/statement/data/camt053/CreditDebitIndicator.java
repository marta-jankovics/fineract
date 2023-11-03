package org.apache.fineract.portfolio.statement.data.camt053;

import org.apache.fineract.portfolio.TransactionEntryType;

public enum CreditDebitIndicator {

    DBIT, CRDT,;

    public CreditDebitIndicator forTransactionEntryType(TransactionEntryType entryType) {
        return entryType == TransactionEntryType.DEBIT ? DBIT : CRDT;
    }
}
