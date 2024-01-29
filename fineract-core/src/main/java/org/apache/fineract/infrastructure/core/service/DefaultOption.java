package org.apache.fineract.infrastructure.core.service;

public interface DefaultOption {

    boolean isDefault();

    static <T extends Enum<T> & DefaultOption> T getDefault(Class<T> clazz) {
        for (T enumConstant : clazz.getEnumConstants()) {
            if (enumConstant.isDefault()) {
                return enumConstant;
            }
        }
        return null;
    }
}
