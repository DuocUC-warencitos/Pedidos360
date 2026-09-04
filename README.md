# DSY1107 — Semana 04 — Sesión 3 — Frontend Angular 21 + MSAL

Este frontend parte del proyecto probado de la Sesión 1 y contiene incorporados los avances de las Sesiones 2 y 3:

- Login/logout mediante Microsoft Entra ID.
- `handleRedirectObservable({ navigateToLoginRequestUrl: false })`.
- Espera de `InteractionStatus.None`.
- `ChangeDetectorRef.markForCheck()` para Angular 21 zoneless.
- Scope `Pedidos.Read`.
- `MsalGuard` para `/protegido`.
- `MsalInterceptor` para `http://localhost:8080/*`.
- Obtención de Access Token.
- `HttpClient` para consultar `GET /api/pedidos`.

## 1. Configuración obligatoria

Editar:

`src/environments/environment.ts`

Reemplazar:

- `PEGAR_AQUI_CLIENT_ID_FRONTEND`
- `PEGAR_AQUI_TENANT_ID`
- `PEGAR_AQUI_CLIENT_ID_API`

En Microsoft Entra ID debe existir en la App Registration del frontend la Redirect URI SPA:

`http://localhost:4200`

La App Registration de la API debe exponer:

`api://<CLIENT_ID_API>/Pedidos.Read`

El frontend debe tener permiso delegado sobre ese scope.

## 2. Ejecutar

```bash
npm install
npm start
```

Abrir `http://localhost:4200`.

> `node_modules`, `.angular/cache`, `dist` y `.git` fueron excluidos intencionalmente del ZIP.
