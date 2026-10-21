import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth-service';

@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {
  authService = inject(AuthService);
  private router = inject(Router);

  // routerLinkActive compara también los query params. Para "Salas/Pistas/Equipos" basta con que
  // coincida la categoría; para "Todo reservable" la URL no debe llevar ningún filtro.
  filtroActivo = { paths: 'subset', queryParams: 'subset', fragment: 'ignored', matrixParams: 'ignored' } as const;
  soloTodo = { paths: 'exact', queryParams: 'exact', fragment: 'ignored', matrixParams: 'ignored' } as const;

  // En pantallas estrechas las dos barras se funden en una con este desplegable
  menuAbierto = signal(false);

  toggleMenu() {
    this.menuAbierto.update(v => !v);
  }

  cerrarMenu() {
    this.menuAbierto.set(false);
  }

  logout() {
    this.authService.logout();
    this.cerrarMenu();
    this.router.navigate(['/login']);
  }
}
