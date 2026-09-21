package io.github.roony11_1.pedidos_service.infrastructure.security;

import io.github.roony11_1.error.core.StandardErrorCategories;
import io.github.roony11_1.error.core.exceptions.AppException;

public class UnauthenticatedException extends AppException {
    public UnauthenticatedException(String message) {
        super("ERR-0001", message, StandardErrorCategories.UNAUTHORIZED, message);
    }
}
