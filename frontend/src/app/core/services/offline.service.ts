import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class OfflineService implements OnDestroy {
  private _isOffline = new BehaviorSubject<boolean>(!navigator.onLine);
  isOffline$ = this._isOffline.pipe(distinctUntilChanged());

  private retryInterval: any = null;
  private onlineDebounce: any = null;

  constructor(private ngZone: NgZone) {
    this.checkConnectivity().then(online => {
      this.ngZone.run(() => {
        this._isOffline.next(!online);
        if (!online) this.startRetry();
      });
    });

    window.addEventListener('offline', this.handleOffline, true);
    window.addEventListener('online', this.handleOnline, true);
  }

  private handleOffline = (): void => {
    clearTimeout(this.onlineDebounce);
    this.ngZone.run(() => {
      this._isOffline.next(true);
      this.startRetry();
    });
  };

  private handleOnline = (): void => {
    clearTimeout(this.onlineDebounce);
    this.onlineDebounce = setTimeout(() => {
      this.checkConnectivity().then(online => {
        this.ngZone.run(() => {
          if (online) {
            this._isOffline.next(false);
            this.stopRetry();
          }
        });
      });
    }, 1200);
  };

  private async checkConnectivity(): Promise<boolean> {
    if (!navigator.onLine) return false;
    try {
      await fetch('https://www.gstatic.com/generate_204', {
        method: 'HEAD',
        mode: 'no-cors',
        cache: 'no-store',
      });
      return true;
    } catch {
      return false;
    }
  }

  private startRetry(): void {
    this.stopRetry();
    this.retryInterval = setInterval(() => {
      this.checkConnectivity().then(online => {
        if (online) {
          this.ngZone.run(() => {
            this._isOffline.next(false);
            this.stopRetry();
          });
        }
      });
    }, 3000);
  }

  private stopRetry(): void {
    if (this.retryInterval) {
      clearInterval(this.retryInterval);
      this.retryInterval = null;
    }
  }

  ngOnDestroy(): void {
    window.removeEventListener('offline', this.handleOffline, true);
    window.removeEventListener('online', this.handleOnline, true);
    clearTimeout(this.onlineDebounce);
    this.stopRetry();
  }
}
