import {
  MsalGuardConfiguration,
  MsalInterceptorConfiguration,
} from '@azure/msal-angular';
import {
  BrowserCacheLocation,
  IPublicClientApplication,
  InteractionType,
  PublicClientApplication,
} from '@azure/msal-browser';

import { environment } from '@env/environment';

export function MSALInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.msal.clientId,
      authority: `https://login.microsoftonline.com/${environment.msal.tenantId}`,
      redirectUri: environment.msal.redirectUri,
      postLogoutRedirectUri: environment.msal.postLogoutRedirectUri
    },
    cache: {
      cacheLocation: BrowserCacheLocation.LocalStorage,
    },
    system: {
      allowPlatformBroker: false,
    },
  });
}

export function MSALGuardConfigFactory(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: [environment.msal.apiScope],
    },
  };
}

export function MSALInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const apiBase = environment.apiGatewayUrl ?? '';
  const apiScope = environment.msal.apiScope;
  const protectedResourceMap = new Map<string, Array<string | { httpMethod: string; scopes: string[] | null }>>();
  // Rutas publicas del catalogo: GET sin token para no forzar login.
  // Deben ir primero: MsalInterceptor usa el primer match y null = bypass.
  protectedResourceMap.set(`${apiBase}/v1/productos`, [
    { httpMethod: 'GET', scopes: null },
  ]);
  protectedResourceMap.set(`${apiBase}/v1/productos/*`, [
    { httpMethod: 'GET', scopes: null },
  ]);
  // Resto del API protegido
  if (apiScope) {
    protectedResourceMap.set(`${apiBase}/*`, [apiScope]);
  }
  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap,
  };
}
