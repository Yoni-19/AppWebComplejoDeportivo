import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ROL_LABEL } from '../../core/models/cliente.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatSidenavModule, MatToolbarModule, MatListModule, MatButtonModule, MatIconModule, MatChipsModule],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss',
})
export class LayoutComponent {
  readonly auth = inject(AuthService);
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
    }
  }
}
