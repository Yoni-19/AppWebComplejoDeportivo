import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { Cancha } from '../../core/models/cancha.model';
import { EntrenamientoAlta } from '../../core/models/entrenamiento.model';
import { PageHeader } from '../../shared/ui/page-header.component';
import { CanchaService } from '../canchas/cancha.service';
import { EntrenamientoService } from './entrenamiento.service';

@Component({
  selector: 'app-entrenamiento-form',
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
  templateUrl: './entrenamiento-form.component.html',
  styleUrl: './entrenamiento-form.component.scss',
})
export class EntrenamientoFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(EntrenamientoService);
  private readonly canchaService = inject(CanchaService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);

  readonly canchas = signal<Cancha[]>([]);
  readonly isEdit = signal(false);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  private editingId: number | null = null;

  readonly form = this.fb.nonNullable.group({
    titulo: ['', Validators.required],
    entrenador: ['', Validators.required],
    descripcion: [''],
    canchaId: [null as number | null, Validators.required],
    inicio: ['', Validators.required],
    fin: ['', Validators.required],
    cupoMaximo: [10, [Validators.required, Validators.min(1)]],
    precio: [0, [Validators.required, Validators.min(0)]],
  });

  constructor() {
    this.canchaService.list().subscribe({
      next: (canchas) => this.canchas.set(canchas.filter((c) => c.activa)),
      error: (err) => this.errorMessage.set(apiErrorMessage(err, 'No se pudieron cargar las canchas')),
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit.set(true);
      this.editingId = Number(idParam);
      this.service.get(this.editingId).subscribe({
        next: (entrenamiento) => {
          if (!entrenamiento) {
            this.errorMessage.set('No existe el entrenamiento solicitado.');
            return;
          }
          this.form.patchValue({
            titulo: entrenamiento.titulo,
            entrenador: entrenamiento.entrenador,
            descripcion: entrenamiento.descripcion ?? '',
            canchaId: entrenamiento.canchaId,
            inicio: entrenamiento.fechaHoraInicio.slice(0, 16),
            fin: entrenamiento.fechaHoraFin.slice(0, 16),
            cupoMaximo: entrenamiento.cupoMaximo,
            precio: entrenamiento.precio,
          });
        },
        error: (err) => {
          this.errorMessage.set(apiErrorMessage(err, 'No se pudo cargar el entrenamiento'));
        },
      });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    if (new Date(value.fin) <= new Date(value.inicio)) {
      this.errorMessage.set('La fecha de fin debe ser posterior a la de inicio.');
      return;
    }

    const request: EntrenamientoAlta = {
      canchaId: value.canchaId!,
      titulo: value.titulo,
      entrenador: value.entrenador,
      descripcion: value.descripcion || undefined,
      fechaHoraInicio: value.inicio,
      fechaHoraFin: value.fin,
      cupoMaximo: value.cupoMaximo,
      precio: value.precio,
    };

    this.loading.set(true);
    this.errorMessage.set(null);

    const request$ = this.isEdit()
      ? this.service.update(this.editingId!, request)
      : this.service.create(request);

    request$.subscribe({
      next: () => {
        this.loading.set(false);
        this.snackBar.open('Entrenamiento guardado.', 'Cerrar', { duration: 3000 });
        this.router.navigate(['/entrenamientos']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(apiErrorMessage(err, 'No se pudo guardar el entrenamiento'));
      },
    });
  }
}
