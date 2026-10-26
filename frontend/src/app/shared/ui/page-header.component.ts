import { Component, input } from '@angular/core';

/** Reusable page title + subtitle + action slot. Used by every feature page. */
@Component({
  selector: 'app-page-header',
  standalone: true,
  template: `
    <div class="page-header">
      <div>
        <h1>{{ title() }}</h1>
        @if (subtitle()) {
          <p>{{ subtitle() }}</p>
        }
      </div>
      <div class="page-header-actions">
        <ng-content />
      </div>
    </div>
  `,
  styles: `
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 16px;
      flex-wrap: wrap;
      margin-bottom: 24px;
    }
    h1 { font-size: 1.5rem; margin: 0; }
    p { margin: 4px 0 0; opacity: 0.7; font-size: 0.9rem; }
    .page-header-actions { display: flex; gap: 8px; }
  `,
})
export class PageHeader {
  readonly title = input.required<string>();
  readonly subtitle = input<string>();
}
