import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { Cancha } from '../../core/models/cancha.model';
import { Cliente } from '../../core/models/cliente.model';
import { Implemento, ImplementoLineaAlta } from '../../core/models/implemento.model';
import { METODOS_PAGO, MetodoPago } from '../../core/models/pago.model';
import { DetalleReserva, EstadoServicio, ReservaAlta } from '../../core/models/reserva.model';
import { AuthService } from '../../core/services/auth.service';
import { PageHeader } from '../../shared/ui/page-header.component';
import { CanchaService } from '../canchas/cancha.service';
import { ClienteRapidoDialogComponent } from '../clientes/cliente-rapido-dialog.component';
import { ClienteService } from '../clientes/cliente.service';
import { ImplementoService } from '../implementos/implemento.service';
import { ReservaService } from './reserva.service';

/** Builds "yyyy-MM-ddTHH:mm" from the LOCAL date, so no timezone shift is applied. */
function toLocalDateTime(date: Date, time: string): string {
  const anio = date.getFullYear();
  const mes = String(date.getMonth() + 1).padStart(2, '0');
  const dia = String(date.getDate()).padStart(2, '0');
  return `${anio}-${mes}-${dia}T${time}`;
}

/** Minutes between two "HH:mm" values; negative when the end is before the start. */
function minutosEntre(inicio: string, fin: string): number {
  const [horaInicio, minutoInicio] = inicio.split(':').map(Number);
  const [horaFin, minutoFin] = fin.split(':').map(Number);
  return (horaFin * 60 + minutoFin) - (horaInicio * 60 + minutoInicio);
}

@Component({
  selector: 'app-reserva-form',
  standalone: true,
  imports: [ReactiveFormsModule, MatCard, MatCardContent, MatInputModule, MatSelectModule, MatDatepickerModule, MatDialogModule, MatButton, MatIconModule, MatChipsModule, MatProgressSpinnerModule, RouterLink, PageHeader, DecimalPipe],
  templateUrl: './reserva-form.component.html',
  styleUrl: './reserva-form.component.scss',
})
export class ReservaFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ReservaService);
  private readonly canchaService = inject(CanchaService);
  private readonly clienteService = inject(ClienteService);
  private readonly implementoService = inject(ImplementoService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  readonly auth = inject(AuthService);

  readonly canchas = signal<Cancha[]>([]);
  readonly clientes = signal<Cliente[]>([]);
  readonly isEdit = signal(false);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  /** Estado actual de la reserva; el backend lo calcula y no se puede enviar. */
  readonly estadoActual = signal<EstadoServicio | null>(null);
  /** Detalle tipo pedido que devuelve el backend al editar. */
  readonly detalle = signal<DetalleReserva[]>([]);
  readonly metodosPago = METODOS_PAGO;
  /** Catálogo de implementos prestables: solo se ofrecen los activos. */
  readonly implementos = signal<Implemento[]>([]);
  /** Implementos pedidos en esta reserva: sin costo salvo que se dañen o se pierdan. */
  readonly pedidos = signal<ImplementoLineaAlta[]>([]);
  readonly implementoParaAgregar = signal<number | null>(null);
  private editingId: number | null = null;

  readonly form = this.fb.nonNullable.group({
    // El cliente no elige usuario: el backend lo fuerza al usuario autenticado.
    usuarioId: [null as number | null],
    canchaId: [null as number | null, Validators.required],
    fecha: [null as Date | null, Validators.required],
    horaInicio: ['10:00', Validators.required],
    horaFin: ['12:00', Validators.required],
    // Solo se usa al crear: registra el anticipo del 50%.
    metodoPago: [null as MetodoPago | null, Validators.required],
  });

  /**
   * Vista previa del cobro con la misma regla del backend: horas iniciadas y
   * anticipo del 50%. Al editar no se muestra porque el total manda desde la API.
   */
  get resumen(): { cancha: string; horas: number; precioHora: number; total: number; anticipo: number; saldo: number } | null {
    const { canchaId, horaInicio, horaFin } = this.form.getRawValue();
    const cancha = this.canchas().find((c) => c.id === canchaId);
    if (!cancha || !horaInicio || !horaFin) return null;

    const minutos = minutosEntre(horaInicio, horaFin);
    if (minutos <= 0) return null;

    const horas = Math.ceil(minutos / 60);
    const total = cancha.precioPorHora * horas;
    const anticipo = Math.round(total * 50) / 100;
    return {
      cancha: cancha.nombre,
      horas,
      precioHora: cancha.precioPorHora,
      total,
      anticipo,
      saldo: total - anticipo,
    };
  }

  constructor() {
    this.canchaService.list().subscribe({
      next: (canchas) => this.canchas.set(canchas.filter((c) => c.activa)),
      error: (err) => this.errorMessage.set(apiErrorMessage(err, 'No se pudieron cargar las canchas')),
    });

    this.implementoService.list().subscribe({
      next: (implementos) => this.implementos.set(implementos.filter((i) => i.activo)),
      error: (err) => this.errorMessage.set(apiErrorMessage(err, 'No se pudieron cargar los implementos')),
    });

    // El rol define si hace falta elegir el cliente; el rol llega de /api/auth/me.
    this.auth.asegurarSesion().subscribe({
      next: () => this.configurarPorRol(),
      error: () => this.configurarPorRol(),
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit.set(true);
      this.editingId = Number(idParam);
      this.service.get(this.editingId).subscribe({
        next: (reserva) => {
          if (!reserva) {
            this.errorMessage.set('No existe la reserva solicitada.');
            return;
          }
          this.estadoActual.set(reserva.estado);
          this.detalle.set(reserva.detalle ?? []);
          this.form.patchValue({
            usuarioId: reserva.usuarioId,
            canchaId: reserva.canchaId,
            fecha: new Date(reserva.fechaHoraInicio),
            horaInicio: reserva.fechaHoraInicio.slice(11, 16),
            horaFin: reserva.fechaHoraFin.slice(11, 16),
          });
          // El backend ignora el usuario al actualizar: se muestra bloqueado.
          this.form.controls.usuarioId.disable();
          // El anticipo ya se registro al crear: no se cobra de nuevo al reprogramar.
          this.form.controls.metodoPago.disable();

          // El backend rechaza reprogramar una reserva cancelada o completada.
          if (reserva.estado === 'CANCELADA' || reserva.estado === 'COMPLETADA') {
            this.form.disable();
            this.errorMessage.set(
              reserva.estado === 'CANCELADA'
                ? 'Esta reserva está cancelada y no se puede reprogramar.'
                : 'Esta reserva está completada y no se puede reprogramar.',
            );
          }
        },
        error: (err) => {
          this.errorMessage.set(apiErrorMessage(err, 'No se pudo cargar la reserva'));
        },
      });
    }
  }

  /** Recepcion elige el cliente; un CLIENTE reserva a su nombre sin ver el campo. */
  private configurarPorRol(): void {
    const usuarioId = this.form.controls.usuarioId;

    if (this.auth.esRecepcionista()) {
      usuarioId.setValidators(Validators.required);
      this.clienteService.listClientes().subscribe({
        next: (clientes) => this.clientes.set(clientes),
        error: (err) => this.errorMessage.set(apiErrorMessage(err, 'No se pudieron cargar los clientes')),
      });
    } else {
      usuarioId.clearValidators();
    }
    usuarioId.updateValueAndValidity({ emitEvent: false });
  }

  /** Alta presencial sin perder cancha, fecha ni horario. */
  registrarCliente(): void {
    if (!this.auth.esRecepcionista() || this.isEdit()) return;

    this.dialog
      .open(ClienteRapidoDialogComponent, { width: '520px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((cliente?: Cliente) => {
        if (!cliente || cliente.id == null) return;

        const nuevo = cliente;
        const nuevoId = cliente.id;
        this.clientes.update((lista) =>
          lista.some((c) => c.id === nuevoId) ? lista : [...lista, nuevo],
        );
        this.form.controls.usuarioId.setValue(nuevoId);
        this.snackBar.open('Cliente registrado.', 'Cerrar', { duration: 2500 });
      });
  }

  /** Implementos del catálogo que todavía no se pidieron: evita líneas duplicadas. */
  get implementosDisponibles(): Implemento[] {
    const usados = new Set(this.pedidos().map((p) => p.implementoId));
    return this.implementos().filter((i) => i.id != null && !usados.has(i.id));
  }

  seleccionarImplemento(implementoId: number | null): void {
    this.implementoParaAgregar.set(implementoId);
  }

  agregarImplemento(): void {
    const implementoId = this.implementoParaAgregar();
    if (implementoId == null) return;
    this.pedidos.update((lista) => [...lista, { implementoId, cantidad: 1 }]);
    this.implementoParaAgregar.set(null);
  }

  quitarImplemento(implementoId: number): void {
    this.pedidos.update((lista) => lista.filter((p) => p.implementoId !== implementoId));
  }

  setCantidadImplemento(implementoId: number, valor: string): void {
    const cantidad = Math.max(1, Number(valor) || 1);
    this.pedidos.update((lista) =>
      lista.map((p) => (p.implementoId === implementoId ? { ...p, cantidad } : p)),
    );
  }

  nombreImplemento(implementoId: number): string {
    return this.implementos().find((i) => i.id === implementoId)?.nombre ?? 'Implemento';
  }

  submit(): void {
    if (this.form.disabled) return;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { usuarioId, canchaId, fecha, horaInicio, horaFin, metodoPago } = this.form.getRawValue();
    if (canchaId == null || fecha == null || (!this.auth.esCliente() && usuarioId == null)) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set(null);

    if (horaFin <= horaInicio) {
      this.errorMessage.set('La hora de fin debe ser posterior a la de inicio');
      return;
    }

    this.loading.set(true);

    const payload: ReservaAlta = {
      fechaHoraInicio: toLocalDateTime(fecha, horaInicio),
      fechaHoraFin: toLocalDateTime(fecha, horaFin),
      canchaId,
    };
    if (usuarioId != null) {
      payload.usuarioId = usuarioId;
    }
    if (!this.isEdit()) {
      payload.metodoPago = metodoPago ?? undefined;
      // Los implementos se envían solo al crear: el préstamo queda como movimiento.
      if (this.pedidos().length > 0) {
        payload.implementos = this.pedidos();
      }
    }

    const request$ = this.isEdit()
      ? this.service.update(this.editingId as number, payload)
      : this.service.create(payload);

    request$.subscribe({
      next: () => {
        this.loading.set(false);
        this.snackBar.open('Reserva guardada.', 'Cerrar', { duration: 3000 });
        this.router.navigate(['/reservas']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(apiErrorMessage(err, 'No se pudo guardar la reserva'));
      },
    });
  }
}
