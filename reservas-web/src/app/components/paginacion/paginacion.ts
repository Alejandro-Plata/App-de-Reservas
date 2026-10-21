import { Component, input, output } from '@angular/core';

// Barra de paginación reutilizable: solo avisa de la página pedida, quien la usa hace la petición
@Component({
  selector: 'app-paginacion',
  templateUrl: './paginacion.html',
  styleUrl: './paginacion.css',
})
export class Paginacion {
  pagina = input.required<number>();      // empieza en 0, como Spring
  totalPaginas = input.required<number>();
  totalElementos = input.required<number>();
  cambio = output<number>();

  ir(n: number) {
    if (n >= 0 && n < this.totalPaginas()) this.cambio.emit(n);
  }
}
