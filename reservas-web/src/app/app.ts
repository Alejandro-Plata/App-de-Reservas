import { Component, inject } from '@angular/core';
import { RouterOutlet, Router, RouterLink } from '@angular/router';
import { Header } from './components/header/header';
import { Footer } from './components/footer/footer';
import { AuthService } from './services/auth-service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, Header, Footer],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private router = inject(Router);
  authService = inject(AuthService);

  // En login y registro no mostramos la columna derecha
  isAuthPage(): boolean {
    const url = this.router.url;
    return url.startsWith('/login') || url.startsWith('/register');
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
