import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private _isDark = false;

  constructor() {
    this._isDark = localStorage.getItem('theme') === 'dark';
    this.apply();
  }

  get isDark(): boolean {
    return this._isDark;
  }

  toggle(): void {
    this._isDark = !this._isDark;
    localStorage.setItem('theme', this._isDark ? 'dark' : 'light');
    this.apply();
  }

  private apply(): void {
    document.body.classList.toggle('dark-theme', this._isDark);
  }
}
