package org.apache.fineract.currentaccount.service;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ID_TYPE_PARAM;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.DefaultOption;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public abstract class IdTypeResolver {

    @NotNull
    public static <T extends Enum<T>> T resolve(@NotNull Class<T> clazz, String idType) {
        if (idType == null) {
            return clazz.isAssignableFrom(DefaultOption.class) ? (T) DefaultOption.getDefault((Class) clazz) : null;
        }
        idType = formatIdType(idType);
        try {
            return Enum.valueOf(clazz, idType);
        } catch (IllegalArgumentException e) {
            throw resolveFailed(idType, e);
        }
    }

    public static String formatIdType(String idType) {
        return idType == null ? null : idType.replaceAll("-", "_").toUpperCase();
    }

    public static RuntimeException resolveFailed(String idType, Exception e) {
        return new PlatformApiDataValidationException("error.msg.id.type.not.found", "Provided type " + idType + " is not supported",
                ID_TYPE_PARAM, e, idType);
    }
}
