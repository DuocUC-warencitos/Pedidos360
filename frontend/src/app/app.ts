import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';

import { RouterLink, RouterOutlet } from '@angular/router';

import { environment } from '../environments/environment';

import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterLink, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App 
{
  accessTokenPreview = '';

  respuestaApi: unknown = null;

  constructor(
    private readonly authService: AuthService,
    private readonly http: HttpClient) 
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
    this.authService.getAccesToken().subscribe(
    {
      next: (token) => 
      {
        this.accessTokenPreview = token.substring(0, 90) + '...';
      },
      error: (error) => 
      {
        console.error(error);
      },
    });
  }

  consultarPedidos(): void 
  {
    this.respuestaApi = null;

    this.http.get(`${environment.apiBaseUrl}/api/pedidos`).subscribe(
    {
      next: (respuesta) => 
      {
        this.respuestaApi = respuesta;
      },
      error: (error) => 
      {
        this.respuestaApi = 
        {
          status: error.status,
          mensaje: 'Solicitud rechazada',
        };
      },
    });
  }

  logout(): void 
  {
    this.authService.logout();
  }
}
