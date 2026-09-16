import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { NgxSonnerToaster } from 'ngx-sonner';

import { LoggingService } from '@core/logging/logging.service';
import { HeaderComponent } from '@shared/layout/header/header';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, HeaderComponent, NgxSonnerToaster],
    templateUrl: './app.html',
    styleUrl: './app.css',
})
export class App implements OnInit
{
    constructor(
        private readonly msalService: MsalService,
        private readonly logger: LoggingService)
    {
    }

    ngOnInit(): void
    {
        this.logger.debug('Inicializando aplicación');

        this.msalService
            .handleRedirectObservable(
			{
                navigateToLoginRequestUrl: true
            })
            .subscribe(
            {
                next: () =>
                {
                    this.logger.debug('Redirect de MSAL procesado');
                },
                error: error =>
                {
                    this.logger.error('Error procesando redirect de MSAL',error);
                }
            });
    }
}