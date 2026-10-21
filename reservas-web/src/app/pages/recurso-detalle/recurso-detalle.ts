import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ResourceService } from '../../services/resource-service';
import { ReservationService } from '../../services/reservation-service';
import { Resource } from '../../types/resource';
import { BusySlot } from '../../types/reservation';
import { formatearHora, hoyLocal, localAIso } from '../../utils/fechas';

// Límites de duración de una reserva (regla 7 de la guía). Deben coincidir con el backend;
// aquí solo sirven para avisar antes de enviar, la validación de verdad la hace la API.
const MIN_MINUTOS = 30;
const MAX_MINUTOS = 240;

@Component({
  selector: 'app-recurso-detalle',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './recurso-detalle.html',
  styleUrl: './recurso-detalle.css',
})
export class RecursoDetalle implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private resourceService = inject(ResourceService);
  private reservationService = inject(ReservationService);

  recurso: Resource | null = null;
  errorMessage = '';
  isLoading = true;

  fecha = hoyLocal();
  ocupados: BusySlot[] = [];
  cargandoHuecos = false;

  reservaForm: FormGroup;
  reservaError = '';
  reservaOk = '';
  enviando = false;

  formatearHora = formatearHora;

  constructor(private fb: FormBuilder) {
    this.reservaForm = this.fb.group({
      startAt: ['', [Validators.required]],
      endAt: ['', [Validators.required]]
    });
  }

  async ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    try {
      this.recurso = await this.resourceService.get(id);
      await this.cargarHuecos();
    } catch (error) {
      this.errorMessage = (error as Error).message;
    } finally {
      this.isLoading = false;
    }
  }

  async cargarHuecos() {
    if (!this.recurso || !this.fecha) return;
    this.cargandoHuecos = true;
    try {
      this.ocupados = await this.resourceService.availability(this.recurso.id, this.fecha);
    } catch (error) {
      this.errorMessage = (error as Error).message;
    } finally {
      this.cargandoHuecos = false;
    }
  }

  // Devuelve un mensaje si las fechas no cumplen las reglas 1 y 7, o '' si todo va bien
  private validarFechas(): string {
    const inicio = new Date(this.reservaForm.value.startAt).getTime();
    const fin = new Date(this.reservaForm.value.endAt).getTime();
    if (fin <= inicio) return 'La hora de fin debe ser posterior a la de inicio.';
    if (inicio < Date.now()) return 'No se puede reservar en el pasado.';
    const minutos = (fin - inicio) / 60000;
    if (minutos < MIN_MINUTOS) return `La reserva mínima es de ${MIN_MINUTOS} minutos.`;
    if (minutos > MAX_MINUTOS) return `La reserva máxima es de ${MAX_MINUTOS / 60} horas.`;
    return '';
  }

  async reservar() {
    this.reservaError = '';
    this.reservaOk = '';
    if (this.reservaForm.invalid || !this.recurso) {
      this.reservaForm.markAllAsTouched();
      return;
    }
    const aviso = this.validarFechas();
    if (aviso) {
      this.reservaError = aviso;
      return;
    }

    this.enviando = true;
    try {
      await this.reservationService.create({
        resourceId: this.recurso.id,
        startAt: localAIso(this.reservaForm.value.startAt),
        endAt: localAIso(this.reservaForm.value.endAt),
      });
      this.reservaOk = '¡Reserva confirmada!';
      this.reservaForm.reset();
      await this.cargarHuecos(); // refrescamos para que aparezca el hueco recién ocupado
    } catch (error) {
      // Aquí llega, por ejemplo, el 409 de solapamiento tal cual lo explica la API
      this.reservaError = (error as Error).message;
    } finally {
      this.enviando = false;
    }
  }

  volver() {
    this.router.navigate(['/recursos']);
  }
}
