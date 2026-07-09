import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { publicGuard } from './core/guards/public.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/invoices', pathMatch: 'full' },
  {
    path: 'login',
    canActivate: [publicGuard],
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'invoices',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/invoices/invoice-list/invoice-list.component')
        .then(m => m.InvoiceListComponent),
  },
  {
    path: 'invoices/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/invoices/invoice-form/invoice-form.component')
        .then(m => m.InvoiceFormComponent),
  },
  {
    path: 'invoices/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/invoices/invoice-detail/invoice-detail.component')
        .then(m => m.InvoiceDetailComponent),
  },
  {
    path: 'invoices/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/invoices/invoice-form/invoice-form.component')
        .then(m => m.InvoiceFormComponent),
  },
  { path: '**', redirectTo: '/invoices' },
];
