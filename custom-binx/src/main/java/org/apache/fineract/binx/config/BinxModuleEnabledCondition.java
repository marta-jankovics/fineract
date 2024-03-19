package org.apache.fineract.binx.config;

import java.util.Optional;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class BinxModuleEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return Optional.ofNullable(context.getEnvironment().getProperty("fineract.module.binx.enabled", Boolean.class)).orElse(false);
    }
}
