import { Component, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent {
  form = new FormGroup({
    username: new FormControl('', [Validators.required, Validators.minLength(3)]),
    password: new FormControl('', [Validators.required, Validators.minLength(8)]),
  });

  isLoading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  isRegistering = signal(false);

  constructor(
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  submit(): void {
    if (this.form.invalid) return;
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const { username, password } = this.form.value;

    if (this.isRegistering()) {
      this.auth.register(username!, password!).subscribe({
        next: () => {
          this.isLoading.set(false);
          this.successMessage.set('¡Cuenta creada! Ahora inicia sesión con tus credenciales.');
          this.isRegistering.set(false);
          this.form.reset();
        },
        error: (err) => {
          this.isLoading.set(false);
          this.errorMessage.set(
            err.status === 409 ? 'El nombre de usuario ya está en uso'
                               : 'Error al crear la cuenta. Inténtalo de nuevo.'
          );
        },
      });
    } else {
      this.auth.login(username!, password!).subscribe({
        next: () => {
          this.isLoading.set(false);
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/invoices';
          this.router.navigate([returnUrl]);
        },
        error: (err) => {
          this.isLoading.set(false);
          this.errorMessage.set(
            err.status === 401 ? 'Credenciales incorrectas'
                               : 'Error al iniciar sesión. Inténtalo de nuevo.'
          );
        },
      });
    }
  }

  toggleMode(): void {
    this.isRegistering.update(v => !v);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.form.reset();
  }
}
