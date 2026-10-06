package io.github.roony11_1.pedidos_service.infrastructure.spec;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import io.github.roony11_1.pedidos_service.core.domain.model.PedidoJob;

public final class PedidoJobSpecifications 
{
    public static Specification<PedidoJob> porId(UUID jobId) 
    {
        return SpecificationFactory.byId(jobId);
    }
}
