package io.github.roony11_1.pedidos_service.infrastructure.spec;

import org.springframework.data.jpa.domain.Specification;

import io.github.roony11_1.specification.core.FilterCondition;
import io.github.roony11_1.specification.core.FilterOperator;
import io.github.roony11_1.specification.spring.FilterSpecificationBuilder;

public final class SpecificationFactory 
{
    public static <T> Specification<T> byId(Object id) 
    {
        return new FilterSpecificationBuilder<T>()
            .withCondition(new FilterCondition("id", FilterOperator.EQ, id))
            .build();
    }
}
