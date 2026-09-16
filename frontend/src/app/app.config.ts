import { ApplicationConfig } from '@angular/core';

import {
	HTTP_INTERCEPTORS,
	provideHttpClient,
	withInterceptorsFromDi
} from '@angular/common/http';

import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import {
  MSAL_GUARD_CONFIG,
  MSAL_INSTANCE,
  MSAL_INTERCEPTOR_CONFIG,
  MsalBroadcastService,
  MsalGuard,
  MsalInterceptor,
  MsalService,
} from '@azure/msal-angular';
import { provideTanStackQuery, QueryClient } from '@tanstack/angular-query-experimental';

import { AUTHENTICATION_PROVIDER } from '@core/auth/authenticationProvider';
import {
  MSALGuardConfigFactory,
  MSALInstanceFactory,
  MSALInterceptorConfigFactory,
} from '@core/auth/msal.config';
import { MsalAuthenticationProvider } from '@core/auth/providers/msalAuthenticationProvider';

export const appConfig: ApplicationConfig = 
{
	providers: 
	[
		provideRouter(routes),
		provideHttpClient(withInterceptorsFromDi()),
		provideTanStackQuery(new QueryClient()),
		{
			provide: AUTHENTICATION_PROVIDER,
			useClass: MsalAuthenticationProvider
		},
		{
			provide: MSAL_INSTANCE,
			useFactory: MSALInstanceFactory
		},
		{
			provide: MSAL_GUARD_CONFIG,
			useFactory: MSALGuardConfigFactory
		},
		{
			provide: MSAL_INTERCEPTOR_CONFIG,
			useFactory: MSALInterceptorConfigFactory
		},
		{
			provide: HTTP_INTERCEPTORS,
			useClass: MsalInterceptor,
			multi: true
		},
		MsalService,
		MsalGuard,
		MsalBroadcastService
	]
};
