import { ApplicationConfig, importProvidersFrom, provideZoneChangeDetection, APP_INITIALIZER } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { providePrimeNG } from 'primeng/config';
import { MessageService } from 'primeng/api';
import Aura from '@primeng/themes/aura';

import { routes } from './app.routes';
import { jwtInterceptor } from './_interceptors/jwt.interceptor';
import { AuthenticationService } from './_services/authentication.service'; // Import the service
import { Observable } from 'rxjs';

import { MarkdownModule } from 'ngx-markdown';
import { GalleriaModule } from 'primeng/galleria'; // Import GalleriaModule
import { DialogModule } from 'primeng/dialog';     // Import DialogModule

import { provideIcons } from '@ng-icons/core';
import { APP_ICONS } from './shared/hero-icons';

// Factory function for APP_INITIALIZER
export function initializeAppFactory(authService: AuthenticationService): () => Observable<any> {
  return () => authService.initializeAuthState();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: Aura,
        options: {
          darkModeSelector: '.dark',
        },
      }
    }),
    // APP_INITIALIZER provider
    {
      provide: APP_INITIALIZER,
      useFactory: initializeAppFactory,
      deps: [AuthenticationService], // Dependencies for the factory
      multi: true // Required for APP_INITIALIZER
    },
    MessageService,
    importProvidersFrom(MarkdownModule.forRoot()), // Import MarkdownModule ngx-markdown
    importProvidersFrom(GalleriaModule), // Provide GalleriaModule
    importProvidersFrom(DialogModule),     // Provide DialogModule

    provideIcons(APP_ICONS)
  ]
};
