import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { Mantenimiento } from '../../core/models/mantenimiento.model';
import { AuthService } from '../../core/services/auth.service';
import { PageHeader } from '../../shared/ui/page-header.component';
import { MantenimientoService } from './mantenimiento.service';
import { ResolverMantenimientoDialogComponent } from './resolver-mantenimiento-dialog.component';

@Component({
  selector: 'app-mantenimiento-list',
  standalone: true,
  imports: [
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    DatePipe,
    PageHeader,
  ],
  templateUrl: './mantenimiento-list.component.html',
  styleUrl: './mantenimiento-list.component.scss',
})
export class MantenimientoListComponent {
  private readonly service = inject(MantenimientoService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  readonly auth = inject(AuthService);

  readonly mantenimientos = signal<Mantenimiento[]>([]);
  readonly loading = signal(true);
  /** El admin gobierna: consulta, pero el bloqueo lo opera mantenimiento. */
  readonly puedeOperar = computed(() => this.auth.esMantenimiento());
  readonly displayedColumns = computed(() => {
    const columnas = ['cancha', 'motivo', 'inicio', 'afectadas', 'solucion', 'estado'];
    return this.puedeOperar() ? [...columnas, 'acciones'] : columnas;
  });

  constructor() {
    if (this.auth.isAuthenticated) {
      this.auth.asegurarSesion().subscribe();
    }
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.service.list().subscribe({
      next: (mantenimientos) => {
        this.mantenimientos.set(mantenimientos);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(apiErrorMessage(err, 'No se pudieron cargar los mantenimientos'), 'Cerrar', {
          duration: 4000,
        });
      },
    });
  }

  resolver(mantenimiento: Mantenimiento): void {
    if (mantenimiento.id == null) return;
    const id = mantenimiento.id;
    this.dialog
      .open(ResolverMantenimientoDialogComponent, {
        width: '480px',
        maxWidth: '95vw',
        data: { canchaNombre: mantenimiento.canchaNombre, motivo: mantenimiento.motivo },
      })
      .afterClosed()
      .subscribe((solucion?: string) => {
        if (!solucion) return;
        this.service.resolver(id, solucion).subscribe({
          next: () => {
            this.snackBar.open('Mantenimiento resuelto.', 'Cerrar', { duration: 2500 });
            this.cargar();
          },
          error: (err) => {
            this.snackBar.open(apiErrorMessage(err, 'No se pudo resolver el mantenimiento'), 'Cerrar', {
              duration: 5000,
            });
          },
        });
      });
  }
}
