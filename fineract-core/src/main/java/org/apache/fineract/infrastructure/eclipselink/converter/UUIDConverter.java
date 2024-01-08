//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.apache.fineract.infrastructure.eclipselink.converter;

import java.util.UUID;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.mappings.foundation.AbstractDirectMapping;
import org.eclipse.persistence.sessions.Session;

public class UUIDConverter implements Converter {

    public UUIDConverter() {}

    public Object convertObjectValueToDataValue(Object uuidValue, Session session) {
        if (uuidValue == null) {
            return (UUID) null;
        }
        if (uuidValue instanceof UUID) {
            return uuidValue;
        } else {
            throw new IllegalArgumentException("Source object is not an instance of java.util.UUID");
        }
    }

    public Object convertDataValueToObjectValue(Object jdbcValue, Session session) {
        if (jdbcValue == null) {
            return (UUID) null;
        }
        if (jdbcValue instanceof UUID) {
            return jdbcValue;
        } else {
            return UUID.fromString(jdbcValue.toString());
        }
    }

    public boolean isMutable() {
        return false;
    }

    public void initialize(DatabaseMapping mapping, Session session) {
        if (mapping.isDirectToFieldMapping() && ((AbstractDirectMapping) mapping).getFieldClassification() == null) {
            AbstractDirectMapping directMapping = (AbstractDirectMapping) mapping;
            Class<?> attributeClassification = mapping.getAttributeClassification();
            if (attributeClassification.isInstance(UUID.class)) {
                directMapping.setFieldClassification(UUID.class);
            }
        }

    }
}
