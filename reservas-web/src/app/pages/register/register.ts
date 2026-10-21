import { Component, inject } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth-service';

// Validador a nivel de grupo: compara dos campos, por eso no va en un solo control
function passwordsIguales(group: AbstractControl): ValidationErrors | null {
  const pass = group.get('password')?.value;
  const confirm = group.get('confirm')?.value;
  return pass === confirm ? null : { noCoinciden: true };
}

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  registerForm: FormGroup;
  isLoading = false;
  errorMessage = '';
  authService = inject(AuthService);
  router = inject(Router);

  constructor(private fb: FormBuilder) {
    this.registerForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirm: ['', [Validators.required]]
    }, { validators: passwordsIguales });
  }

  isFieldInvalid(field: string): boolean {
    const control = this.registerForm.get(field);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  onSubmit() {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';

    this.authService.register(this.registerForm.value.email, this.registerForm.value.password).then(() => {
      this.isLoading = false;
      this.router.navigate(['/recursos']);
    }).catch((error: Error) => {
      this.isLoading = false;
      this.errorMessage = error instanceof TypeError ? 'No se puede conectar con la API. ¿Está arrancada?' : error.message;
    });
  }
}
