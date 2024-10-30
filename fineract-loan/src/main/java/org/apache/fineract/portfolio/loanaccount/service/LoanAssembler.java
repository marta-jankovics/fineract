package org.apache.fineract.portfolio.loanaccount.service;

import java.util.Map;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.portfolio.fund.domain.Fund;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.useradministration.domain.AppUser;

public interface LoanAssembler {

    Loan assembleFrom(Long accountId);

    Loan assembleFrom(JsonCommand command);

    void setHelpers(Loan loanAccount);

    void accountNumberGeneration(JsonCommand command, Loan loan);

    CodeValue findCodeValueByIdIfProvided(Long codeValueId);

    Fund findFundByIdIfProvided(Long fundId);

    Staff findLoanOfficerByIdIfProvided(Long loanOfficerId);

    Map<String, Object> updateFrom(JsonCommand command, Loan loan);

    Map<String, Object> updateLoanApplicationAttributesForWithdrawal(Loan loan, JsonCommand command, AppUser currentUser);

    Map<String, Object> updateLoanApplicationAttributesForRejection(Loan loan, JsonCommand command, AppUser currentUser);
}
