import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { ActivatedRoute, RouterLink } from '@angular/router';
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
  private readonly route = inject(ActivatedRoute);
  readonly auth = inject(AuthService);

  /** La barra lateral separa Clientes de Personal; la ruta decide cual se muestra. */
  readonly vista: 'clientes' | 'personal' =
    this.route.snapshot.data['vista'] === 'personal' ? 'personal' : 'clientes';

  /** Lista completa que devuelve la API: el admin recibe clientes y personal. */
  readonly usuarios = signal<Cliente[]>([]);
  /** Recepcion solo recibe CLIENTE, asi que su vista siempre es la de clientes. */
  readonly lista = computed(() =>
    this.usuarios().filter((c) =>
      this.vista === 'personal' ? (c.rol ?? 'CLIENTE') !== 'CLIENTE' : (c.rol ?? 'CLIENTE') === 'CLIENTE',
    ),
  );
  readonly loading = signal(true);
  /** En Personal no se ofrece CLIENTE: el alta de clientes vive en la otra vista. */
  readonly roles: Rol[] = ['ADMIN', 'RECEPCIONISTA', 'CAJA', 'UTILERIA', 'MANTENIMIENTO'];
  readonly etiquetasRol = ROL_LABEL;
  /** El borrado y "Mi perfil" son solo para ADMIN. */
  readonly puedeGestionar = this.auth.esAdmin;
  /** El recepcionista puede crear y corregir clientes. */
  readonly puedeCrear = this.auth.esStaff;
  readonly puedeEditar = this.auth.esStaff;
  /** El rol solo se edita en Personal; en Clientes la columna no aporta. */
  readonly displayedColumns = computed(() =>
    this.vista === 'personal'
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
      next: (usuarios) => {
        this.usuarios.set(usuarios);
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
        this.usuarios.update((list) => list.map((c) => (c.id === actualizado.id ? { ...c, rol: actualizado.rol } : c)));
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
        this.usuarios.update((list) => list.filter((c) => c.id !== cliente.id));
        this.snackBar.open('Cliente eliminado.', 'Cerrar', { duration: 2500 });
      },
      error: (err) => {
        this.snackBar.open(apiErrorMessage(err, 'No se pudo eliminar el cliente'), 'Cerrar', { duration: 5000 });
      },
    });
  }
}
