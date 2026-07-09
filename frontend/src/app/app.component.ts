import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ThemeService } from './core/services/theme.service';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterModule, MatToolbarModule, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  constructor(
    public theme: ThemeService,
    public auth: AuthService,
    private router: Router,
  ) {}

  /**
   * Calls POST /auth/logout so the backend clears the HttpOnly cookie (Max-Age=0),
   * then navigates to /login.  Navigation happens in the subscribe callback so it
   * occurs after the local auth state is cleared, even if the network request fails.
   */
  logout(): void {
    this.auth.logout().subscribe({
      complete: () => this.router.navigate(['/login']),
    });
  }
}
