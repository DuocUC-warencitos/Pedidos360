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
  
  // 🛠️ PARCHE PARA ENTORNOS HTTP (EVITA EL ERROR CRYPTO_NONEXISTENT)
  if (typeof window !== 'undefined' && !window.crypto) {
    (window as any).crypto = {
      getRandomValues: (bucket: any) => {
        for (let i = 0; i < bucket.length; i++) {
          bucket[i] = Math.floor(Math.random() * 256);
        }
        return bucket;
      },
      subtle: {} as any
    };
  } else if (typeof window !== 'undefined' && window.crypto && !window.crypto.subtle) {
    // Si crypto existe pero subtle está bloqueado por el navegador por ser HTTP
    (window.crypto as any).subtle = {} as any;
  }

  return new PublicClientApplication({
    auth: {
      clientId: environment.msal.clientId,
      authority: `https://login.microsoftonline.com/${environment.msal.tenantId}`,
      redirectUri: environment.msal.redirectUri,
      postLogoutRedirectUri: environment.msal.redirectUri,
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
  const protectedResourceMap = new Map<string, Array<string>>();
  protectedResourceMap.set(`${environment.apiGatewayUrl}/*`, [
    environment.msal.apiScope,
  ]);
  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap,
  };
}
