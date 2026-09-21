import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { isDevMode, inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { ErrorResponse, isErrorResponse } from '@core/api/error-response';
import { LoggingService } from '@core/logging/logging.service';
import { NotificationService } from '@core/notification/notification.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notify = inject(NotificationService);
  const logger = inject(LoggingService);

  // Evita spam en polling de job (si el job no existe aún, no notificar)
  const silentUrls = ['/saga/jobs/'];
  const isSilent = silentUrls.some((u) => req.url.includes(u));

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      // No toastear silenciosos (pero sí loggear)
      if (isSilent && err.status === 404) {
        logger.warn(`HTTP 404 silencioso ${req.url}`, err.error);
        return throwError(() => err);
      }

      // Network / CORS / Gateway caído
      if (err.status === 0) {
        logger.error('NetworkError', { url: req.url, error: err });
        notify.error('Sin conexión', 'Verifica tu red o que el gateway esté activo');
        return throwError(() => err);
      }

      // Parsear ErrorResponse (roony-error-spring)
      let body: ErrorResponse | null = null;
      if (err.error && typeof err.error === 'object' && isErrorResponse(err.error)) {
        body = err.error as ErrorResponse;
      } else if (typeof err.error === 'string' && err.error.length > 0 && err.error.length < 800) {
        // fallback texto plano
        body = { code: `HTTP-${err.status}`, message: err.error, timestamp: new Date().toISOString(), path: req.url, traceId: null, details: null };
      }

      const code = body?.code ?? `HTTP-${err.status}`;
      const message = body?.message ?? err.message ?? `Error ${err.status}`;
      const path = body?.path ?? req.url;
      const traceId = body?.traceId ?? null;
      const details = body?.details ?? null;

      // Log siempre completo
      logger.error(`HTTP ${err.status} ${code} @ ${path}`, { code, message, traceId, path, details, status: err.status, url: req.url });

      // Dev mode muestra ruta, traceId y detalles; prod solo mensaje y código
      const dev = isDevMode();
      let description: string | undefined;

      if (dev) {
        const parts: string[] = [];
        parts.push(code);
        if (path) parts.push(path);
        if (traceId) parts.push(`traceId: ${traceId}`);
        if (details && details.length > 0) parts.push(details.join(' | '));
        description = parts.join(' · ');
      } else {
        // prod: solo código (si distinto del mensaje) no exponer path/trace
        description = code !== message ? code : undefined;
      }

      // Mapa UX simple por código/status
      // 401 ya lo maneja MSAL redirect, pero si llega del API lo mostramos
      // 403 es el caso CLIENTE -> avanzarEstado
      // 409 StockInsuficiente / concurrencia, 400 validación
      notify.error(message, description);

      return throwError(() => err);
    }),
  );
};
