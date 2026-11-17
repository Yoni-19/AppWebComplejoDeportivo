import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ROL_LABEL } from '../../core/models/cliente.model';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionService } from '../../core/services/notificacion.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatSidenavModule, MatToolbarModule, MatListModule, MatButtonModule, MatIconModule, MatChipsModule, MatDividerModule, MatBadgeModule],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss',
})
export class LayoutComponent {
  readonly auth = inject(AuthService);
  readonly notificaciones = inject(NotificacionService);
  private readonly breakpoints = inject(BreakpointObserver);

  /** Sidenav as overlay on small screens, fixed sidebar on desktop. */
  readonly isMobile = toSignal(this.breakpoints.observe([Breakpoints.Handset, Breakpoints.TabletPortrait]), {
    initialValue: { matches: false, breakpoints: {} },
  });

  readonly drawer = signal(false);

  readonly rolLabel = computed(() => {
    const rol = this.auth.sesion()?.rol;
    return rol ? ROL_LABEL[rol] : '';
  });

  constructor() {
    // El rol no viaja en el token: se pide a /api/auth/me al entrar al layout.
    if (this.auth.isAuthenticated) {
      this.auth.asegurarSesion().subscribe({ error: () => this.auth.logout() });
      this.notificaciones.contarNoLeidas().subscribe({ error: () => undefined });
    }
  }
}
