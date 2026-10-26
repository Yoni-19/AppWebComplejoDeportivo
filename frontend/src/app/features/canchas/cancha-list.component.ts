import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { Cancha } from '../../core/models/cancha.model';
import { PageHeader } from '../../shared/ui/page-header.component';
import { CanchaService } from './cancha.service';

@Component({
  selector: 'app-cancha-list',
  standalone: true,
  imports: [RouterLink, MatCard, MatCardContent, MatIconModule, MatButtonModule, MatChipsModule, MatProgressSpinnerModule, DecimalPipe, PageHeader],
  templateUrl: './cancha-list.component.html',
  styleUrl: './cancha-list.component.scss',
})
export class CanchaListComponent {
  private readonly service = inject(CanchaService);
  private readonly snackBar = inject(MatSnackBar);

  readonly canchas = signal<Cancha[]>([]);
  readonly loading = signal(true);

  constructor() {
    this.service.list().subscribe({
      next: (canchas) => {
        this.canchas.set(canchas);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(apiErrorMessage(err, 'No se pudieron cargar las canchas'), 'Cerrar', { duration: 4000 });
      },
    });
  }

  alternarEstado(cancha: Cancha): void {
    if (cancha.id == null) return;
    this.service.update({ ...cancha, activa: !cancha.activa }).subscribe({
      next: (actualizada) => {
        this.canchas.update((list) => list.map((c) => (c.id === actualizada.id ? actualizada : c)));
        this.snackBar.open('Estado actualizado.', 'Cerrar', { duration: 2500 });
      },
      error: (err) => {
        this.snackBar.open(apiErrorMessage(err, 'No se pudo actualizar la cancha'), 'Cerrar', { duration: 4000 });
      },
    });
  }
}
