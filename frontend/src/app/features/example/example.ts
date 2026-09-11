import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { environment } from '../../../environments/environment';
import { LoggingService } from '../../core/logging/logging.service';


@Component({
    selector: 'app-example',
    imports: 
    [
        CommonModule,
        RouterLink,
        RouterOutlet
    ],
    templateUrl: './example.html',
    styleUrl: './example.css',
})
export class Example 
{
    accessTokenPreview = '';

    respuestaApi: unknown = null;

    constructor(
        private readonly authService: AuthService,
        private readonly http: HttpClient,
        private readonly logger: LoggingService) 
    {
    }

    get user() 
    {
        return this.authService.user;
    }

    login(): void 
    {
        this.authService.login();
    }

    obtenerAccessToken(): void 
    {

        this.authService
        .getAccesToken()
        .subscribe(
        {
            next: token => 
            {
                this.accessTokenPreview = token.toString();
            },
            error: error => 
            {
                this.logger.error('Error al obtener el access token', error);
            }
        });
    }

    consultarPedidos(): void 
    {
        this.respuestaApi = null;

        this.http
        .get(`${environment.apiBaseUrl}/api/pedidos`)
        .subscribe(
        {
            next: respuesta => 
            {
                this.respuestaApi = respuesta;
            },
            error: error => 
            {
                this.respuestaApi = 
                {
                    status: error.status,
                    mensaje: 'Solicitud rechazada'
                };
            }
        });
    }

    logout(): void 
    {
        this.authService.logout();
    }
}