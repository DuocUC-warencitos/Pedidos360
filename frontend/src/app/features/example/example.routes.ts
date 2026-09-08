import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';

export const exampleRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./example')
        .then(m => m.Example),

    children: [
      {
        path: 'protegido',
        canActivate: [MsalGuard],
        loadComponent: () =>
          import('./components/protegido/protegido')
            .then(m => m.Protegido)
      }
    ]
  }
];