package io.github.roony11_1.pedidos_service.infrastructure.client;

import io.github.roony11_1.error.core.ErrorCategory;
import io.github.roony11_1.error.core.exceptions.AppException;
import io.github.roony11_1.error.rest.HttpStatusRegistry;

public class ProductoServiceUnavailableException extends AppException
{
    public static final ErrorCategory CATEGORY = new ErrorCategory() {
        @Override
        public String name() { return "SERVICE_UNAVAILABLE"; }
        @Override
        public String description() { return "Service Unavailable"; }
    };

    static {
        HttpStatusRegistry.register(CATEGORY, 503);
    }

    public ProductoServiceUnavailableException(String msg, Throwable cause)
    {
        super("ERR-0500", msg, CATEGORY, msg, cause);
    }
}