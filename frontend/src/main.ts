import { registerLocaleData } from '@angular/common';
import localeEsCl from '@angular/common/locales/es-CL';
import { bootstrapApplication } from '@angular/platform-browser';

import { App } from './app/app';
import { appConfig } from './app/app.config';

// "es-CL" es el LOCALE_ID por defecto de la aplicación (ver app.config.ts).
// Angular solo incluye los datos de "en-US" de fábrica; sin este registro,
// DatePipe, CurrencyPipe, DecimalPipe y PercentPipe lanzan NG0701.
registerLocaleData(localeEsCl);

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
