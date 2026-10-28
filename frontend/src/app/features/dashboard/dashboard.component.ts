import { Component, inject, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { CanchaService } from '../canchas/cancha.service';
import { ClienteService } from '../clientes/cliente.service';
import { ReservaService } from '../reservas/reserva.service';
import { PageHeader } from '../../shared/ui/page-header.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, MatCard, MatCardContent, MatIconModule, MatButton, MatChipsModule, PageHeader],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  readonly auth = inject(AuthService);
  private readonly reservasService = inject(ReservaService);
  private readonly canchasService = inject(CanchaService);
  private readonly clientesService = inject(ClienteService);

  readonly reservasEnCurso = signal(0);
  readonly reservasConfirmadas = signal(0);
  readonly totalReservas = signal(0);
  readonly canchasActivas = signal(0);
  readonly totalClientes = signal(0);

  /** First name for the greeting, falling back to the e-mail. */
  readonly nombre = signal('');

  constructor() {
    // El saludo y los accesos dependen del rol, y el rol llega de /api/auth/me.
    this.auth.asegurarSesion().subscribe({
      next: (sesion) => {
        this.nombre.set(sesion.nombres?.split(' ')[0] ?? sesion.correo);
        this.cargar();
      },
      error: () => this.cargar(),
    });
  }

  private cargar(): void {
    // El backend ya filtra: un CLIENTE recibe solo sus propias reservas.
    this.reservasService.list().subscribe((reservas) => {
      this.totalReservas.set(reservas.length);
      this.reservasEnCurso.set(reservas.filter((r) => r.estado === 'EN_CURSO').length);
      this.reservasConfirmadas.set(reservas.filter((r) => r.estado === 'CONFIRMADA').length);
    });

    // Global counters only make sense (and are only allowed) for staff.
    if (!this.auth.esCliente()) {
      this.canchasService.list().subscribe((canchas) => {
        this.canchasActivas.set(canchas.filter((c) => c.activa).length);
      });
      this.clientesService.listClientes().subscribe((clientes) => {
        this.totalClientes.set(clientes.length);
      });
    }
  }
}
