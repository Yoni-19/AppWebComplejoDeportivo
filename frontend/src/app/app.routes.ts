import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { CajaComponent } from './features/caja/caja.component';
import { CanchaFormComponent } from './features/canchas/cancha-form.component';
import { CanchaListComponent } from './features/canchas/cancha-list.component';
import { ClienteFormComponent } from './features/clientes/cliente-form.component';
import { ClienteListComponent } from './features/clientes/cliente-list.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { EntrenamientoDetailComponent } from './features/entrenamientos/entrenamiento-detail.component';
import { EntrenamientoFormComponent } from './features/entrenamientos/entrenamiento-form.component';
import { EntrenamientoListComponent } from './features/entrenamientos/entrenamiento-list.component';
import { ImplementoListComponent } from './features/implementos/implemento-list.component';
import { LoginComponent } from './features/login/login.component';
import { MantenimientoFormComponent } from './features/mantenimiento/mantenimiento-form.component';
import { MantenimientoListComponent } from './features/mantenimiento/mantenimiento-list.component';
import { NotificacionesListComponent } from './features/notificaciones/notificaciones-list.component';
import { RegistroComponent } from './features/registro/registro.component';
import { ReservaFormComponent } from './features/reservas/reserva-form.component';
import { ReservaListComponent } from './features/reservas/reserva-list.component';
import { UtileriaComponent } from './features/utileria/utileria.component';
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
      {
        path: 'reservas',
        component: ReservaListComponent,
        canActivate: [roleGuard('ADMIN', 'RECEPCIONISTA', 'CAJA', 'CLIENTE')],
      },
      {
        path: 'reservas/nueva',
        component: ReservaFormComponent,
        canActivate: [roleGuard('RECEPCIONISTA', 'CLIENTE')],
      },
      {
        path: 'reservas/:id/editar',
        component: ReservaFormComponent,
        canActivate: [roleGuard('RECEPCIONISTA', 'CLIENTE')],
      },
      { path: 'canchas', component: CanchaListComponent, canActivate: [roleGuard('ADMIN')] },
      { path: 'canchas/nueva', component: CanchaFormComponent, canActivate: [roleGuard('ADMIN')] },
      { path: 'canchas/:id/editar', component: CanchaFormComponent, canActivate: [roleGuard('ADMIN')] },
      {
        path: 'clientes',
        component: ClienteListComponent,
        canActivate: [roleGuard('ADMIN', 'RECEPCIONISTA')],
        data: { vista: 'clientes' },
      },
      {
        path: 'personal',
        component: ClienteListComponent,
        canActivate: [roleGuard('ADMIN')],
        data: { vista: 'personal' },
      },
      {
        path: 'personal/nueva',
        component: ClienteFormComponent,
        canActivate: [roleGuard('ADMIN')],
        data: { vista: 'personal' },
      },
      {
        path: 'personal/:id/editar',
        component: ClienteFormComponent,
        canActivate: [roleGuard('ADMIN')],
        data: { vista: 'personal' },
      },
      {
        path: 'clientes/nueva',
        component: ClienteFormComponent,
        canActivate: [roleGuard('ADMIN', 'RECEPCIONISTA')],
      },
      {
        path: 'clientes/:id/editar',
        component: ClienteFormComponent,
        canActivate: [roleGuard('ADMIN', 'RECEPCIONISTA')],
      },
      {
        path: 'entrenamientos',
        component: EntrenamientoListComponent,
        canActivate: [roleGuard('ADMIN', 'RECEPCIONISTA', 'CLIENTE')],
      },
      {
        path: 'entrenamientos/nueva',
        component: EntrenamientoFormComponent,
        canActivate: [roleGuard('RECEPCIONISTA')],
      },
      {
        path: 'entrenamientos/:id/editar',
        component: EntrenamientoFormComponent,
        canActivate: [roleGuard('RECEPCIONISTA')],
      },
      {
        path: 'entrenamientos/:id',
        component: EntrenamientoDetailComponent,
        canActivate: [roleGuard('ADMIN', 'RECEPCIONISTA', 'CLIENTE')],
      },
      {
        path: 'mantenimiento',
        component: MantenimientoListComponent,
        canActivate: [roleGuard('ADMIN', 'RECEPCIONISTA', 'MANTENIMIENTO')],
      },
      {
        path: 'mantenimiento/nuevo',
        component: MantenimientoFormComponent,
        canActivate: [roleGuard('MANTENIMIENTO')],
      },
      {
        path: 'notificaciones',
        component: NotificacionesListComponent,
      },
      {
        // Solo utileria: la recepcion mueve el turno, no el prestamo.
        path: 'utileria',
        component: UtileriaComponent,
        canActivate: [roleGuard('UTILERIA')],
      },
      {
        path: 'caja',
        component: CajaComponent,
        canActivate: [roleGuard('CAJA')],
      },
      {
        path: 'implementos',
        component: ImplementoListComponent,
        canActivate: [roleGuard('ADMIN')],
      },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
