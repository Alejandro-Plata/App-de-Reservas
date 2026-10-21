import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ReservationService } from '../../services/reservation-service';
import { ResourceService } from '../../services/resource-service';
import { Reservation, ReservationFilters } from '../../types/reservation';
import { Resource } from '../../types/resource';
import { Paginacion } from '../../components/paginacion/paginacion';
import { formatearFecha } from '../../utils/fechas';

@Component({
  selector: 'app-admin-reservas',
  imports: [FormsModule, Paginacion],
  templateUrl: './admin-reservas.html',
  styleUrl: './admin-reservas.css',
})
export class AdminReservas implements OnInit {
  private reservationService = inject(ReservationService);
  private resourceService = inject(ResourceService);

  reservas: Reservation[] = [];
  recursos: Resource[] = []; // para rellenar el desplegable de filtro
  filtros: ReservationFilters = { status: '', resourceId: null, from: '', to: '' };

  pagina = 0;
  totalPaginas = 0;
  totalElementos = 0;
  readonly TAM_PAGINA = 10;
  isLoading = false;
  errorMessage = '';

  formatearFecha = formatearFecha;

  async ngOnInit() {
    // El desplegable no necesita paginar bien: pedimos una página grande (el backend la limitará)
    try {
      this.recursos = (await this.resourceService.list(0, 100)).content;
    } catch (error) {
      console.log(error);
    }
    await this.cargar(0);
  }

  async cargar(pagina = this.pagina) {
    this.isLoading = true;
    this.errorMessage = '';
    try {
      const res = await this.reservationService.all(pagina, this.TAM_PAGINA, this.filtros);
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

  limpiar() {
    this.filtros = { status: '', resourceId: null, from: '', to: '' };
    this.cargar(0);
  }

  async cancelar(r: Reservation) {
    if (!confirm(`¿Cancelar la reserva de ${r.userEmail} en "${r.resourceName}"?`)) return;
    try {
      await this.reservationService.cancel(r.id);
      await this.cargar();
    } catch (error) {
      this.errorMessage = (error as Error).message;
    }
  }
}
