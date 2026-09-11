package io.github.roony11_1.pedidos_service.infrastructure.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import io.github.roony11_1.pedidos_service.kernel.IUserTokenService;

@Component 
public class SpringSecurityUserTokenService implements IUserTokenService
{
    @Override
    public String getUserId()
    {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt))
            throw new IllegalStateException("No hay un usuario autenticado");

        return jwt.getSubject();
    }
}
