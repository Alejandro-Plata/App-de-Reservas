import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth-service';
import { Page } from '../types/page';
import { Reservation, ReservationFilters, ReservationRequest } from '../types/reservation';

@Injectable({
  providedIn: 'root',
})
export class ReservationService {
  private auth = inject(AuthService);

  async create(data: ReservationRequest): Promise<Reservation> {
    const response = await this.auth.request('/reservations', { method: 'POST', body: JSON.stringify(data) });
    if (!response.ok) throw new Error(await this.auth.leerError(response, 'Error al crear la reserva'));
    return response.json();
  }

  async mine(page: number, size: number): Promise<Page<Reservation>> {
    const params = new URLSearchParams({ page: String(page), size: String(size), sort: 'startAt,desc' });
    const response = await this.auth.request(`/reservations/me?${params}`);
    if (!response.ok) throw new Error(await this.auth.leerError(response, 'Error al cargar tus reservas'));
    return response.json();
  }

  // Solo ADMIN. Los filtros vacíos no se envían
  async all(page: number, size: number, filters: ReservationFilters): Promise<Page<Reservation>> {
    const params = new URLSearchParams({ page: String(page), size: String(size), sort: 'startAt,desc' });
    if (filters.status) params.set('status', filters.status);
    if (filters.resourceId) params.set('resourceId', String(filters.resourceId));
    if (filters.from) params.set('from', filters.from);
    if (filters.to) params.set('to', filters.to);
    const response = await this.auth.request(`/reservations?${params}`);
    if (!response.ok) throw new Error(await this.auth.leerError(response, 'Error al cargar las reservas'));
    return response.json();
  }

  async cancel(id: number): Promise<Reservation> {
    const response = await this.auth.request(`/reservations/${id}/cancel`, { method: 'PATCH' });
    if (!response.ok) throw new Error(await this.auth.leerError(response, 'Error al cancelar la reserva'));
    return response.json();
  }
}
