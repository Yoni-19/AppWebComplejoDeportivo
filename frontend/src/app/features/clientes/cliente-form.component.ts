import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { PageHeader } from '../../shared/ui/page-header.component';
import { ClienteService } from './cliente.service';

@Component({
  selector: 'app-cliente-form',
  standalone: true,
  imports: [ReactiveFormsModule, MatCard, MatCardContent, MatInputModule, MatButton, MatIconModule, MatChipsModule, MatProgressSpinnerModule, RouterLink, PageHeader],
  templateUrl: './cliente-form.component.html',
  styleUrl: './cliente-form.component.scss',
})
export class ClienteFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ClienteService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);

  readonly isEdit = signal(false);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  private editingId: number | null = null;

  readonly form = this.fb.nonNullable.group({
    nombres: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    apellidos: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    correo: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    clave: ['', [Validators.minLength(2), Validators.maxLength(100)]],
  });

  constructor() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit.set(true);
      this.editingId = Number(idParam);
      this.service.get(this.editingId).subscribe({
        next: (cliente) => {
          if (!cliente) {
            this.errorMessage.set('No existe el cliente solicitado.');
            return;
          }
          this.form.patchValue({
            nombres: cliente.nombres,
            apellidos: cliente.apellidos,
            correo: cliente.correo,
          });
        },
        error: (err) => {
          this.errorMessage.set(apiErrorMessage(err, 'No se pudo cargar el cliente'));
        },
      });
    } else {
      // Alta: UsuarioRequest exige la clave.
      this.form.controls.clave.setValidators([
        Validators.required,
        Validators.minLength(2),
        Validators.maxLength(100),
      ]);
      this.form.controls.clave.updateValueAndValidity();
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const { nombres, apellidos, correo, clave } = this.form.getRawValue();
    const request$ = this.isEdit()
      ? this.service.update({ id: this.editingId, nombres, apellidos, correo }, clave)
      : this.service.create({ nombres, apellidos, correo, clave });

    request$.subscribe({
      next: () => {
        this.loading.set(false);
        this.snackBar.open('Cliente guardado.', 'Cerrar', { duration: 3000 });
        this.router.navigate(['/clientes']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(apiErrorMessage(err, 'No se pudo guardar el cliente'));
      },
    });
  }
}
