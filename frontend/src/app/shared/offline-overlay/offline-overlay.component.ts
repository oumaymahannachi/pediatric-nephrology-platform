import { Component, ChangeDetectorRef, NgZone, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { OfflineService } from '../../core/services/offline.service';

type OverlayState = 'hidden' | 'offline' | 'reconnected';

@Component({
  selector: 'app-offline-overlay',
  standalone: true,
  imports: [CommonModule],
  template: `
    <!-- OFFLINE WALL -->
    <div class="offline-wall" *ngIf="state === 'offline'">
      <div class="offline-card">
        <div class="icon-circle">
          <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="1" y1="1" x2="23" y2="23"/>
            <path d="M16.72 11.06A10.94 10.94 0 0 1 19 12.55"/>
            <path d="M5 12.55a10.94 10.94 0 0 1 5.17-2.39"/>
            <path d="M10.71 5.05A16 16 0 0 1 22.56 9"/>
            <path d="M1.42 9a15.91 15.91 0 0 1 4.7-2.88"/>
            <path d="M8.53 16.11a6 6 0 0 1 6.95 0"/>
            <circle cx="12" cy="20" r="1" fill="currentColor"/>
          </svg>
        </div>
        <h1 class="title">No Internet Connection</h1>
        <p class="subtitle">You are currently offline. Please check your network connection.</p>
        <div class="dots">
          <span></span><span></span><span></span>
        </div>
        <p class="waiting">Waiting for connection to restore...</p>
      </div>
    </div>

    <!-- RECONNECTED TOAST -->
    <div class="reconnected-toast" *ngIf="state === 'reconnected'" [class.fade-out]="fadingOut">
      <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none"
        stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
        <polyline points="22 4 12 14.01 9 11.01"/>
      </svg>
      <span>You're back online!</span>
    </div>
  `,
  styles: [`
    .offline-wall {
      position: fixed;
      inset: 0;
      z-index: 99999;
      background: linear-gradient(135deg, #0f172a 0%, #1e3a5f 50%, #0c2340 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      animation: wallIn 0.25s ease-out both;
    }
    @keyframes wallIn {
      from { opacity: 0; }
      to   { opacity: 1; }
    }

    .offline-card {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      padding: 56px 48px;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 24px;
      backdrop-filter: blur(20px);
      max-width: 440px;
      width: 90%;
      box-shadow: 0 32px 80px rgba(0, 0, 0, 0.5);
      animation: cardIn 0.35s cubic-bezier(0.34, 1.56, 0.64, 1) both;
    }
    @keyframes cardIn {
      from { transform: translateY(30px) scale(0.96); opacity: 0; }
      to   { transform: translateY(0) scale(1); opacity: 1; }
    }

    .icon-circle {
      width: 112px;
      height: 112px;
      border-radius: 50%;
      background: rgba(239, 68, 68, 0.15);
      border: 2px solid rgba(239, 68, 68, 0.4);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #f87171;
      margin-bottom: 28px;
      animation: pulse 2s ease-in-out infinite;
    }
    @keyframes pulse {
      0%, 100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.3); }
      50%      { box-shadow: 0 0 0 20px rgba(239, 68, 68, 0); }
    }

    .title {
      font-size: 26px;
      font-weight: 800;
      color: #f1f5f9;
      margin: 0 0 12px;
      letter-spacing: -0.5px;
    }
    .subtitle {
      font-size: 15px;
      color: #94a3b8;
      line-height: 1.6;
      margin: 0 0 32px;
    }

    .dots {
      display: flex;
      gap: 8px;
      margin-bottom: 16px;
    }
    .dots span {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      animation: bounce 1.4s infinite ease-in-out;
    }
    .dots span:nth-child(1) { animation-delay: 0s; background: #ef4444; }
    .dots span:nth-child(2) { animation-delay: 0.2s; background: #f59e0b; }
    .dots span:nth-child(3) { animation-delay: 0.4s; background: #3b82f6; }
    @keyframes bounce {
      0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
      40%           { transform: scale(1.2); opacity: 1; }
    }

    .waiting {
      font-size: 13px;
      color: #64748b;
      margin: 0;
      font-style: italic;
    }

    .reconnected-toast {
      position: fixed;
      top: 24px;
      left: 50%;
      transform: translateX(-50%);
      z-index: 99999;
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 14px 28px;
      background: #059669;
      color: #fff;
      font-size: 15px;
      font-weight: 600;
      border-radius: 12px;
      box-shadow: 0 8px 32px rgba(5, 150, 105, 0.4);
      animation: toastIn 0.3s ease-out both;
      transition: opacity 0.5s ease, transform 0.5s ease;
    }
    .reconnected-toast.fade-out {
      opacity: 0;
      transform: translateX(-50%) translateY(-20px);
    }
    @keyframes toastIn {
      from { opacity: 0; transform: translateX(-50%) translateY(-20px); }
      to   { opacity: 1; transform: translateX(-50%) translateY(0); }
    }
  `]
})
export class OfflineOverlayComponent implements OnDestroy {
  state: OverlayState = 'hidden';
  fadingOut = false;
  private sub: Subscription;
  private fadeTimer: any;
  private wasEverOffline = false;

  constructor(
    private offlineService: OfflineService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {
    this.sub = this.offlineService.isOffline$.subscribe(offline => {
      this.ngZone.run(() => {
        clearTimeout(this.fadeTimer);
        this.fadeTimer = null;

        if (offline) {
          this.wasEverOffline = true;
          this.state = 'offline';
          this.fadingOut = false;
        } else if (this.wasEverOffline && (this.state === 'offline' || this.state === 'reconnected')) {
          this.state = 'reconnected';
          this.fadingOut = false;

          this.fadeTimer = setTimeout(() => {
            this.fadingOut = true;
            this.cdr.detectChanges();

            this.fadeTimer = setTimeout(() => {
              this.state = 'hidden';
              this.wasEverOffline = false;
              this.cdr.detectChanges();
            }, 600);
          }, 2500);
        }

        this.cdr.detectChanges();
      });
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    clearTimeout(this.fadeTimer);
  }
}
