import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type Theme = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private themeSubject = new BehaviorSubject<Theme>('light');
  public theme$ = this.themeSubject.asObservable();

  constructor() {
    this.initializeTheme();
  }

  private initializeTheme(): void {
    const savedTheme = localStorage.getItem('theme') as Theme | null;
    if (savedTheme) {
      this.setTheme(savedTheme, false); // Don't save again on init
    } else {
      // If no theme is saved, check the user's system preference
      const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
      this.setTheme(prefersDark ? 'dark' : 'light', true); // Save the initial derived theme
    }
  }

  private setTheme(theme: Theme, save: boolean): void {
    this.themeSubject.next(theme);

    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }

    if (save) {
      localStorage.setItem('theme', theme);
    }
  }

  toggleTheme(): void {
    this.setTheme(this.themeSubject.value === 'light' ? 'dark' : 'light', true);
  }

  getCurrentTheme(): Theme {
    return this.themeSubject.value;
  }
}
