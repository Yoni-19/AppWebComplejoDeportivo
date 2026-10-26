import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { CanchaFormComponent } from './features/canchas/cancha-form.component';
import { CanchaListComponent } from './features/canchas/cancha-list.component';
import { ClienteFormComponent } from './features/clientes/cliente-form.component';
import { ClienteListComponent } from './features/clientes/cliente-list.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { LoginComponent } from './features/login/login.component';
import { RegistroComponent } from './features/registro/registro.component';
import { ReservaFormComponent } from './features/reservas/reserva-form.component';
import { ReservaListComponent } from './features/reservas/reserva-list.component';
import { LayoutComponent } from './shared/layout/layout.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'registro', component: RegistroComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'reservas', component: ReservaListComponent },
      { path: 'reservas/nueva', component: ReservaFormComponent },
      {
        path: 'reservas/:id/editar',
        component: ReservaFormComponent,
        canActivate: [roleGuard('ADMIN', 'RECEPCIONISTA', 'CLIENTE')],
      },
      { path: 'canchas', component: CanchaListComponent, canActivate: [roleGuard('ADMIN')] },
      { path: 'canchas/nueva', component: CanchaFormComponent, canActivate: [roleGuard('ADMIN')] },
      { path: 'canchas/:id/editar', component: CanchaFormComponent, canActivate: [roleGuard('ADMIN')] },
      {
        path: 'clientes',
        component: ClienteListComponent,
        canActivate: [roleGuard('ADMIN', 'RECEPCIONISTA')],
      },
      { path: 'clientes/nueva', component: ClienteFormComponent, canActivate: [roleGuard('ADMIN')] },
      { path: 'clientes/:id/editar', component: ClienteFormComponent, canActivate: [roleGuard('ADMIN')] },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
