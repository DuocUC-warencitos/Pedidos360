package io.github.roony11_1.pedidos_service.infrastructure.security;

import java.util.List;
import java.util.Optional;

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
            throw new UnauthenticatedException("No hay un usuario autenticado");

        return jwt.getSubject();
    }

    @Override
    public String getAuditComentario(String prefijo)
    {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt))
            throw new UnauthenticatedException("No hay un usuario autenticado");

        List<String> roles = jwt.getClaimAsStringList("roles");
        String role = Optional.ofNullable(roles)
            .filter(l -> !l.isEmpty())
            .map(l -> l.get(0))
            .orElse("UNKNOWN");

        String nameClaim = jwt.getClaimAsString("name");
        String firstName = "UNKNOWN";
        if (nameClaim != null && !nameClaim.isBlank()) {
            String[] parts = nameClaim.trim().split("\\s+");
            if (parts.length > 0 && !parts[0].isBlank()) {
                firstName = parts[0];
            }
        }

        // Normaliza a mayúsculas para "ADMIN RICARDO"
        role = role.toUpperCase();
        firstName = firstName.toUpperCase();

        return String.format("%s %s %s", prefijo, role, firstName);
    }
}
