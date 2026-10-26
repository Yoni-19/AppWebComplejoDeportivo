import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { TIPOS_DEPORTE } from '../../core/models/cancha.model';
import { PageHeader } from '../../shared/ui/page-header.component';
import { CanchaService } from './cancha.service';

@Component({
  selector: 'app-cancha-form',
  standalone: true,
  imports: [ReactiveFormsModule, MatCard, MatCardContent, MatInputModule, MatSelectModule, MatSlideToggleModule, MatButton, MatIconModule, MatChipsModule, MatProgressSpinnerModule, RouterLink, PageHeader],
  templateUrl: './cancha-form.component.html',
  styleUrl: './cancha-form.component.scss',
})
export class CanchaFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(CanchaService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);

  readonly deportes = TIPOS_DEPORTE;
  readonly isEdit = signal(false);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  private editingId: number | null = null;

  readonly form = this.fb.nonNullable.group({
    nombre: ['', Validators.required],
    deporte: [TIPOS_DEPORTE[0], Validators.required],
    precioPorHora: [0, [Validators.required, Validators.min(1)]],
    activa: [true],
  });

  constructor() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit.set(true);
      this.editingId = Number(idParam);
      this.service.get(this.editingId).subscribe({
        next: (cancha) => {
          if (!cancha) {
            this.errorMessage.set('No existe la cancha solicitada.');
            return;
          }
          this.form.patchValue({
            nombre: cancha.nombre,
            deporte: cancha.deporte,
            precioPorHora: cancha.precioPorHora,
            activa: cancha.activa,
          });
        },
        error: (err) => {
          this.errorMessage.set(apiErrorMessage(err, 'No se pudo cargar la cancha'));
        },
      });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const value = this.form.getRawValue();
    const request$ = this.isEdit()
      ? this.service.update({ ...value, id: this.editingId })
      : this.service.create(value);

    request$.subscribe({
      next: () => {
        this.loading.set(false);
        this.snackBar.open('Cancha guardada.', 'Cerrar', { duration: 3000 });
        this.router.navigate(['/canchas']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(apiErrorMessage(err, 'No se pudo guardar la cancha'));
      },
    });
  }
}
