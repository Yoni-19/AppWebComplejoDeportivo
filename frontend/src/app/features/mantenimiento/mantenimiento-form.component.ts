import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { Cancha } from '../../core/models/cancha.model';
import { PageHeader } from '../../shared/ui/page-header.component';
import { CanchaService } from '../canchas/cancha.service';
import { MantenimientoService } from './mantenimiento.service';

@Component({
  selector: 'app-mantenimiento-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCard,
    MatCardContent,
    MatInputModule,
    MatSelectModule,
    MatButton,
    MatIconModule,
    MatProgressSpinnerModule,
    RouterLink,
    PageHeader,
  ],
  templateUrl: './mantenimiento-form.component.html',
  styleUrl: './mantenimiento-form.component.scss',
})
export class MantenimientoFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(MantenimientoService);
  private readonly canchaService = inject(CanchaService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly canchas = signal<Cancha[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    canchaId: [null as number | null, Validators.required],
    motivo: ['', Validators.required],
    // Opcional: vacío significa "desde ahora".
    inicio: [''],
  });

  constructor() {
    this.canchaService.list().subscribe({
      next: (canchas) => this.canchas.set(canchas),
      error: (err) => this.errorMessage.set(apiErrorMessage(err, 'No se pudieron cargar las canchas')),
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.loading.set(true);
    this.errorMessage.set(null);

    this.service
      .abrir({
        canchaId: value.canchaId!,
        motivo: value.motivo,
        fechaHoraInicio: value.inicio || null,
      })
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.snackBar.open('Bloqueo registrado.', 'Cerrar', { duration: 3000 });
          this.router.navigate(['/mantenimiento']);
        },
        error: (err) => {
          this.loading.set(false);
          this.errorMessage.set(apiErrorMessage(err, 'No se pudo registrar el bloqueo'));
        },
      });
  }
}
