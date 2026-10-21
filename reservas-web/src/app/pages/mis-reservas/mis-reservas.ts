import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ReservationService } from '../../services/reservation-service';
import { Reservation } from '../../types/reservation';
import { Paginacion } from '../../components/paginacion/paginacion';
import { formatearFecha } from '../../utils/fechas';

@Component({
  selector: 'app-mis-reservas',
  imports: [RouterLink, Paginacion],
  templateUrl: './mis-reservas.html',
  styleUrl: './mis-reservas.css',
})
export class MisReservas implements OnInit {
  private reservationService = inject(ReservationService);

  reservas: Reservation[] = [];
  pagina = 0;
  totalPaginas = 0;
  totalElementos = 0;
  readonly TAM_PAGINA = 8;
  isLoading = false;
  errorMessage = '';

  formatearFecha = formatearFecha;

  ngOnInit() {
    this.cargar();
  }

  async cargar(pagina = this.pagina) {
    this.isLoading = true;
    this.errorMessage = '';
    try {
      const res = await this.reservationService.mine(pagina, this.TAM_PAGINA);
      this.reservas = res.content;
      this.pagina = res.number;
      this.totalPaginas = res.totalPages;
      this.totalElementos = res.totalElements;
    } catch (error) {
      this.errorMessage = error instanceof TypeError ? 'No se puede conectar con la API. ¿Está arrancada?' : (error as Error).message;
    } finally {
      this.isLoading = false;
    }
  }

  async cancelar(r: Reservation) {
    if (!confirm(`¿Cancelar la reserva de "${r.resourceName}"?`)) return;
    try {
      await this.reservationService.cancel(r.id);
      await this.cargar(); // recargamos: el estado pasa a CANCELLED, no desaparece
    } catch (error) {
      this.errorMessage = (error as Error).message;
    }
  }
}
