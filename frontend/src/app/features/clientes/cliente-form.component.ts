import { Component, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable, switchMap, throwError } from 'rxjs';
import { apiErrorMessage } from '../../core/http/api-error';
import { Cliente, Rol, ROL_LABEL } from '../../core/models/cliente.model';
import { AuthService } from '../../core/services/auth.service';
import { PageHeader } from '../../shared/ui/page-header.component';
import { ClienteService } from './cliente.service';

/** En edición la clave es opcional: vacía significa "no cambiarla". */
function claveOpcional(control: AbstractControl): ValidationErrors | null {
  const valor: string = control.value ?? '';
  if (valor === '') return null;
  return valor.length < 2 ? { minlength: { requiredLength: 2, actualLength: valor.length } } : null;
}

@Component({
  selector: 'app-cliente-form',
  standalone: true,
  imports: [ReactiveFormsModule, MatCard, MatCardContent, MatInputModule, MatButton, MatIconModule, MatChipsModule, MatProgressSpinnerModule, MatSelectModule, RouterLink, PageHeader],
  templateUrl: './cliente-form.component.html',
  styleUrl: './cliente-form.component.scss',
})
export class ClienteFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ClienteService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);
  private readonly auth = inject(AuthService);

  readonly isEdit = signal(false);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  /** La ruta de Personal crea y edita personal; la de Clientes, clientes. */
  readonly esPersonal = this.route.snapshot.data['vista'] === 'personal';
  /** En el alta de personal se elige el rol operativo; CLIENTE no se ofrece. */
  readonly roles: Rol[] = ['RECEPCIONISTA', 'ADMIN', 'CAJA', 'UTILERIA', 'MANTENIMIENTO'];
  readonly etiquetasRol = ROL_LABEL;
  readonly rolSeleccionado = signal<Rol>('RECEPCIONISTA');
  /** El admin edita su propia cuenta desde "Mi perfil", no desde la lista de clientes. */
  readonly esMiPerfil = computed(() => {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    return id > 0 && this.auth.sesion()?.id === id;
  });
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
      // Edición: la clave es opcional; vacía deja la actual.
      this.form.controls.clave.setValidators([claveOpcional, Validators.maxLength(100)]);
      this.form.controls.clave.updateValueAndValidity();
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
    let request$: Observable<Cliente>;

    if (this.isEdit()) {
      request$ = this.service.update({ id: this.editingId, nombres, apellidos, correo }, clave);
    } else if (this.esPersonal) {
      // POST /api/usuarios siempre crea CLIENTE: el rol se asigna en un segundo paso.
      request$ = this.service.create({ nombres, apellidos, correo, clave }).pipe(
        switchMap((creado) => {
          if (creado.id == null) {
            return throwError(() => new Error('El alta no devolvió el id del usuario.'));
          }
          return this.service.cambiarRol(creado.id, this.rolSeleccionado());
        }),
      );
    } else {
      request$ = this.service.create({ nombres, apellidos, correo, clave });
    }

    request$.subscribe({
      next: () => {
        this.loading.set(false);
        this.snackBar.open(this.mensajeGuardado(), 'Cerrar', { duration: 3000 });
        this.router.navigate([this.esPersonal ? '/personal' : '/clientes']);
      },
      error: (err) => {
        this.loading.set(false);
        const porDefecto = this.esPersonal ? 'No se pudo guardar el personal' : 'No se pudo guardar el cliente';
        this.errorMessage.set(apiErrorMessage(err, porDefecto));
      },
    });
  }

  private mensajeGuardado(): string {
    if (this.esMiPerfil()) return 'Datos guardados.';
    return this.esPersonal ? 'Personal guardado.' : 'Cliente guardado.';
  }
}
