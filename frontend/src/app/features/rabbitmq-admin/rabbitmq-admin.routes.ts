import { Routes } from '@angular/router';

export const rabbitmqAdminRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./rabbitmq-admin.component')
        .then(m => m.RabbitmqAdminComponent)
  }
];