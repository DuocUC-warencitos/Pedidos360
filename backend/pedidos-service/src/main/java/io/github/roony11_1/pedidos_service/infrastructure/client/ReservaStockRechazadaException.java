package io.github.roony11_1.pedidos_service.infrastructure.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import feign.FeignException;
import io.github.roony11_1.error.core.StandardErrorCategories;
import io.github.roony11_1.error.core.exceptions.AppException;

/**
 * El stock no se pudo reservar por una causa de negocio (409 stock insuficiente,
 * 404 producto, 400 validación). No debe reintentarse ni contar para el
 * circuit breaker: el pedido va directo a FAILED con mensaje limpio.
 */
public class ReservaStockRechazadaException extends AppException
{
    private static final Pattern MESSAGE_JSON =
        Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    public ReservaStockRechazadaException(String message, Throwable cause)
    {
        super("ERR-1001", message, StandardErrorCategories.ALREADY_EXISTS, message, cause);
    }

    public static ReservaStockRechazadaException fromFeign(FeignException ex)
    {
        return new ReservaStockRechazadaException(extractMessage(ex), ex);
    }

    /**
     * Si la causa raíz es un 4xx de producto-service, devuelve su mensaje
     * limpio (sin "[409] during [POST]..." ni URLs internas). Si no, null.
     */
    public static String mensajeLimpioDeCausa(Throwable t)
    {
        Throwable c = t;
        while (c != null)
        {
            if (c instanceof FeignException fe && fe.status() >= 400 && fe.status() < 500)
                return fromFeign(fe).getMessage();
            c = c.getCause();
        }
        return null;
    }

    private static String extractMessage(FeignException ex)
    {
        try
        {
            String body = ex.contentUTF8();
            if (body != null && !body.isBlank())
            {
                Matcher m = MESSAGE_JSON.matcher(body);
                if (m.find())
                    return unescape(m.group(1));
            }
        }
        catch (Exception ignored)
        {
            // cae al fallback genérico
        }
        return "Stock insuficiente (producto-service respondió " + ex.status() + ")";
    }

    private static String unescape(String s)
    {
        return s.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", " ");
    }
}
