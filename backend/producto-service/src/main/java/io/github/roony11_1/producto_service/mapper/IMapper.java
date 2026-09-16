package io.github.roony11_1.producto_service.mapper;

@FunctionalInterface 
public interface IMapper<E, D> 
{
    D map(E entity);
}