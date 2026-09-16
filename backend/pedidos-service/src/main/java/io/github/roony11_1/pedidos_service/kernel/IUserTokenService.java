package io.github.roony11_1.pedidos_service.kernel;
 
public interface IUserTokenService 
{
    String getUserId();

    /**
     * Construye el comentario de auditoría tipo "Estado actualizado por ADMIN RICARDO"
     * usando los claims "roles" (primer rol) y "name" (primer nombre) del JWT.
     * @param prefijo ej. "Estado actualizado por" o "Cancelado por"
     */
    String getAuditComentario(String prefijo);
}
