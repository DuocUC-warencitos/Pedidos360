import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';

import {
  MSAL_INSTANCE,
  MsalBroadcastService,
  MsalService
} from '@azure/msal-angular';

import { MSALInstanceFactory } from './msal-config';

export const appConfig: ApplicationConfig = {

  providers: [

    provideRouter(routes),

    {
      provide: MSAL_INSTANCE,
      useFactory: MSALInstanceFactory
    },

    MsalService,
    MsalBroadcastService

  ]
};
