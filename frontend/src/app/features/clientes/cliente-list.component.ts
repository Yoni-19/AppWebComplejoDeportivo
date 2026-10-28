import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { Cliente, Rol, ROL_LABEL } from '../../core/models/cliente.model';
import { AuthService } from '../../core/services/auth.service';
import { PageHeader } from '../../shared/ui/page-header.component';
import { ClienteService } from './cliente.service';

@Component({
  selector: 'app-cliente-list',
  standalone: true,
  imports: [RouterLink, MatTableModule, MatButtonModule, MatIconModule, MatChipsModule, MatSelectModule, MatProgressSpinnerModule, PageHeader],
  templateUrl: './cliente-list.component.html',
  styleUrl: './cliente-list.component.scss',
})
export class ClienteListComponent {
  private readonly service = inject(ClienteService);
  private readonly snackBar = inject(MatSnackBar);
  readonly auth = inject(AuthService);

  readonly clientes = signal<Cliente[]>([]);
  readonly loading = signal(true);
  readonly roles: Rol[] = ['ADMIN', 'RECEPCIONISTA', 'CLIENTE'];
  readonly etiquetasRol = ROL_LABEL;
  /** El rol, el borrado y "Mi perfil" son solo para ADMIN. */
  readonly puedeGestionar = this.auth.esAdmin;
  /** El recepcionista puede crear y corregir clientes. */
  readonly puedeCrear = this.auth.esStaff;
  readonly puedeEditar = this.auth.esStaff;
  /** El recepcionista solo ve clientes, así que la columna de rol no aporta. */
  readonly displayedColumns = computed(() =>
    this.puedeGestionar()
      ? ['id', 'nombre', 'correo', 'rol', 'acciones']
      : ['id', 'nombre', 'correo', 'acciones'],
  );
  /** Guarda el id mientras se confirma el cambio de rol en el backend. */
  readonly cambiando = signal<number | null>(null);

  constructor() {
    this.cargar();
  }

  private cargar(): void {
    this.service.list().subscribe({
      next: (clientes) => {
        this.clientes.set(clientes);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(apiErrorMessage(err, 'No se pudieron cargar los clientes'), 'Cerrar', { duration: 4000 });
      },
    });
  }

  cambiarRol(cliente: Cliente, rol: Rol): void {
    if (cliente.id == null || cliente.rol === rol) return;

    this.cambiando.set(cliente.id);
    this.service.cambiarRol(cliente.id, rol).subscribe({
      next: (actualizado) => {
        this.clientes.update((list) => list.map((c) => (c.id === actualizado.id ? { ...c, rol: actualizado.rol } : c)));
        this.cambiando.set(null);
        this.snackBar.open('Rol actualizado.', 'Cerrar', { duration: 2500 });
      },
      error: (err) => {
        this.cambiando.set(null);
        // Se recarga para descartar el valor que el usuario intentó seleccionar.
        this.cargar();
        this.snackBar.open(apiErrorMessage(err, 'No se pudo cambiar el rol'), 'Cerrar', { duration: 5000 });
      },
    });
  }

  eliminar(cliente: Cliente): void {
    if (cliente.id == null) return;
    this.service.remove(cliente.id).subscribe({
      next: () => {
        this.clientes.update((list) => list.filter((c) => c.id !== cliente.id));
        this.snackBar.open('Cliente eliminado.', 'Cerrar', { duration: 2500 });
      },
      error: (err) => {
        this.snackBar.open(apiErrorMessage(err, 'No se pudo eliminar el cliente'), 'Cerrar', { duration: 5000 });
      },
    });
  }
}
