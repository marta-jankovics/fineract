package org.apache.fineract.infrastructure.core.jersey.converter;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import org.apache.fineract.currentaccount.enumeration.product.CurrentProductIdType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
public class CurrentProductIdTypeParamConverterProvider implements ParamConverterProvider {

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType,
                                              Annotation[] annotations) {
        if (rawType.equals(CurrentProductIdType.class))
            return (ParamConverter<T>) new CurrentProductIdTypeConverter();
        return null;
    }

}
