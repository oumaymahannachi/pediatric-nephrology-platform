import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { OfflineOverlayComponent } from './shared/offline-overlay/offline-overlay.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, OfflineOverlayComponent],
  template: `
    <router-outlet />
    <app-offline-overlay />
  `,
  styles: [`:host { display: block; min-height: 100vh; }`]
})
export class AppComponent {}
