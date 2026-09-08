import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'example',
    loadChildren: () =>
      import('./features/example/example.routes')
        .then(m => m.exampleRoutes)
  }
];