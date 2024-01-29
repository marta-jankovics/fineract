package org.apache.fineract.currentaccount.service.account;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountIdType;
import org.apache.fineract.currentaccount.service.IdTypeResolver;
import org.apache.fineract.interoperation.domain.InteropIdentifierType;

@Getter
public class CurrentAccountIdTypeResolver {

    CurrentAccountIdType currentType;
    InteropIdentifierType interopType;

    protected CurrentAccountIdTypeResolver(CurrentAccountIdType currentType, InteropIdentifierType interopType) {
        this.currentType = currentType;
        this.interopType = interopType;
    }

    @NotNull
    public static CurrentAccountIdTypeResolver resolveDefault() {
        return new CurrentAccountIdTypeResolver(CurrentAccountIdType.ID, null);
    }

    @NotNull
    public static CurrentAccountIdTypeResolver resolve(String idType) {
        if (idType == null) {
            return resolveDefault();
        }
        idType = IdTypeResolver.formatIdType(idType);
        CurrentAccountIdType currentType = CurrentAccountIdType.resolveName(idType);
        InteropIdentifierType interopType = null;
        if (currentType == null) {
            interopType = InteropIdentifierType.resolveName(idType);
            if (interopType == null) {
                throw IdTypeResolver.resolveFailed(idType, null);
            }
        }
        return new CurrentAccountIdTypeResolver(currentType, interopType);
    }

    public boolean isSecondaryIdentifier() {
        return interopType != null;
    }
}
