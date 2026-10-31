import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { apiErrorMessage } from '../../core/http/api-error';
import { Implemento } from '../../core/models/implemento.model';
import { ConfirmDialogComponent } from '../../shared/ui/confirm-dialog.component';
import { PageHeader } from '../../shared/ui/page-header.component';
import { ImplementoService } from './implemento.service';

/**
 * ABM del catálogo (solo ADMIN). No hay stock: el complejo administra el
 * inventario físico y esta pantalla solo define qué se presta y cuánto cuesta reponerlo.
 */
@Component({
  selector: 'app-implemento-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCard,
    MatCardContent,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    CurrencyPipe,
    PageHeader,
  ],
  templateUrl: './implemento-list.component.html',
  styleUrl: './implemento-list.component.scss',
})
export class ImplementoListComponent {
  private readonly service = inject(ImplementoService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  readonly implementos = signal<Implemento[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly displayedColumns = ['nombre', 'valorReposicion', 'activo', 'acciones'];

  readonly form = this.fb.nonNullable.group({
    nombre: ['', Validators.required],
    valorReposicion: [0, [Validators.required, Validators.min(0)]],
    activo: [true],
  });

  constructor() {
    this.cargar();
  }

  get editando(): boolean {
    return this.editingId() != null;
  }

  cargar(): void {
    this.loading.set(true);
    this.service.list().subscribe({
      next: (implementos) => {
        this.implementos.set(implementos);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(apiErrorMessage(err, 'No se pudieron cargar los implementos'), 'Cerrar', { duration: 4000 });
      },
    });
  }

  editar(implemento: Implemento): void {
    if (implemento.id == null) return;
    this.editingId.set(implemento.id);
    this.form.setValue({
      nombre: implemento.nombre,
      valorReposicion: implemento.valorReposicion,
      activo: implemento.activo,
    });
  }

  cancelarEdicion(): void {
    this.editingId.set(null);
    this.form.reset({ nombre: '', valorReposicion: 0, activo: true });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const datos = this.form.getRawValue();
    const id = this.editingId();
    this.saving.set(true);

    const request$ = id != null ? this.service.update(id, datos) : this.service.create(datos);
    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('Implemento guardado.', 'Cerrar', { duration: 2500 });
        this.cancelarEdicion();
        this.cargar();
      },
      error: (err) => {
        this.saving.set(false);
        this.snackBar.open(apiErrorMessage(err, 'No se pudo guardar el implemento'), 'Cerrar', { duration: 4000 });
      },
    });
  }

  /** Se activa o desactiva en vez de borrar: los movimientos siguen apuntando al implemento. */
  alternarEstado(implemento: Implemento): void {
    if (implemento.id == null) return;
    this.service
      .update(implemento.id, {
        nombre: implemento.nombre,
        valorReposicion: implemento.valorReposicion,
        activo: !implemento.activo,
      })
      .subscribe({
        next: (actualizado) => {
          this.implementos.update((list) => list.map((i) => (i.id === actualizado.id ? actualizado : i)));
        },
        error: (err) => {
          this.snackBar.open(apiErrorMessage(err, 'No se pudo actualizar el implemento'), 'Cerrar', { duration: 4000 });
        },
      });
  }

  eliminar(implemento: Implemento): void {
    if (implemento.id == null) return;
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          titulo: 'Eliminar implemento',
          mensaje: `¿Eliminar "${implemento.nombre}" del catálogo?`,
          confirmar: 'Eliminar',
        },
      })
      .afterClosed()
      .subscribe((confirmado?: boolean) => {
        if (!confirmado || implemento.id == null) return;
        this.service.remove(implemento.id).subscribe({
          next: () => {
            this.snackBar.open('Implemento eliminado.', 'Cerrar', { duration: 2500 });
            this.cargar();
          },
          error: (err) => {
            this.snackBar.open(apiErrorMessage(err, 'No se pudo eliminar el implemento'), 'Cerrar', { duration: 4000 });
          },
        });
      });
  }
}
