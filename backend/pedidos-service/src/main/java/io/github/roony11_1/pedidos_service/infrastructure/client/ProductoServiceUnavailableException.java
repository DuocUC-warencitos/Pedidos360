package io.github.roony11_1.pedidos_service.infrastructure.client;

public class ProductoServiceUnavailableException extends RuntimeException 
{
    public ProductoServiceUnavailableException(String msg, Throwable cause) 
    { 
        super(msg, cause); 
    }
}