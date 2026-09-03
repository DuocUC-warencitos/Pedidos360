import {
  BrowserCacheLocation,
  IPublicClientApplication,
  PublicClientApplication
} from '@azure/msal-browser';

import { environment } from '../environments/environment';

export function MSALInstanceFactory(): IPublicClientApplication {

  return new PublicClientApplication({

    auth: {
      clientId: environment.msal.clientId,

      authority:
        `https://login.microsoftonline.com/${environment.msal.tenantId}`,

      redirectUri: environment.msal.redirectUri,

      postLogoutRedirectUri:
        environment.msal.redirectUri
    },

    cache: {
      cacheLocation: BrowserCacheLocation.LocalStorage
    },

    system: {
      allowPlatformBroker: false
    }
  });
}
