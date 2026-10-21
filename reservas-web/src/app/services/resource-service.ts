import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth-service';
import { Page } from '../types/page';
import { Resource, ResourceCategory, ResourceRequest } from '../types/resource';
import { BusySlot } from '../types/reservation';

@Injectable({
  providedIn: 'root',
})
export class ResourceService {
  private auth = inject(AuthService);

  async list(page: number, size: number, search = '', onlyActive = false, category: ResourceCategory | null = null): Promise<Page<Resource>> {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (search) params.set('name', search);
    if (onlyActive) params.set('active', 'true');
    if (category) params.set('category', category);
    const response = await this.auth.request(`/resources?${params}`);
    if (!response.ok) throw new Error(await this.auth.leerError(response, 'Error al cargar los recursos'));
    return response.json();
  }

  async get(id: number): Promise<Resource> {
    const response = await this.auth.request(`/resources/${id}`);
    if (!response.ok) throw new Error(await this.auth.leerError(response, 'Recurso no encontrado'));
    return response.json();
  }

  async create(data: ResourceRequest): Promise<Resource> {
    const response = await this.auth.request('/resources', { method: 'POST', body: JSON.stringify(data) });
    if (!response.ok) throw new Error(await this.auth.leerError(response, 'Error al crear el recurso'));
    return response.json();
  }

  async update(id: number, data: ResourceRequest): Promise<Resource> {
    const response = await this.auth.request(`/resources/${id}`, { method: 'PUT', body: JSON.stringify(data) });
    if (!response.ok) throw new Error(await this.auth.leerError(response, 'Error al editar el recurso'));
    return response.json();
  }

  // DELETE no borra: solo marca el recurso como inactivo (regla 4 de la guía)
  async deactivate(id: number): Promise<void> {
    const response = await this.auth.request(`/resources/${id}`, { method: 'DELETE' });
    if (!response.ok) throw new Error(await this.auth.leerError(response, 'Error al desactivar el recurso'));
  }

  // date en formato yyyy-MM-dd; devuelve los huecos que YA están ocupados ese día
  async availability(id: number, date: string): Promise<BusySlot[]> {
    const response = await this.auth.request(`/resources/${id}/availability?date=${date}`);
    if (!response.ok) throw new Error(await this.auth.leerError(response, 'Error al consultar la disponibilidad'));
    return response.json();
  }
}
