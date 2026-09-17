package io.github.roony11_1.pedidos_service.core.domain.mapper;

@FunctionalInterface 
public interface IMapper<T, R>
{
    T map(R source);
}
