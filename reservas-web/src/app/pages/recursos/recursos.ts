import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth-service';
import { ResourceService } from '../../services/resource-service';
import { Resource, ResourceCategory } from '../../types/resource';
import { Paginacion } from '../../components/paginacion/paginacion';

@Component({
  selector: 'app-recursos',
  imports: [FormsModule, ReactiveFormsModule, RouterLink, Paginacion],
  templateUrl: './recursos.html',
  styleUrl: './recursos.css',
})
export class Recursos implements OnInit {
  resourceService = inject(ResourceService);
  authService = inject(AuthService);

  private route = inject(ActivatedRoute);

  // Texto para el título y las insignias según la categoría
  readonly NOMBRES: Record<ResourceCategory, string> = { SALA: 'Salas', PISTA: 'Pistas', EQUIPO: 'Equipos' };
  readonly CATEGORIAS: ResourceCategory[] = ['SALA', 'PISTA', 'EQUIPO'];
  categoria: ResourceCategory | null = null;

  recursos: Resource[] = [];
  pagina = 0;
  totalPaginas = 0;
  totalElementos = 0;
  readonly TAM_PAGINA = 6;
  busqueda = '';
  isLoading = false;
  errorMessage = '';

  // Formulario de admin: sirve para crear y para editar (editandoId distingue)
  formAbierto = false;
  editandoId: number | null = null;
  resourceForm: FormGroup;
  formError = '';

  constructor(private fb: FormBuilder) {
    this.resourceForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      description: [''],
      category: ['SALA', [Validators.required]],
      capacity: [1, [Validators.required, Validators.min(1)]]
    });
  }

  ngOnInit() {
    // La franja de categorías del header cambia ?categoria=... sin recargar el componente,
    // por eso nos suscribimos en vez de leer el valor una sola vez.
    this.route.queryParamMap.subscribe(params => {
      const valor = params.get('categoria');
      this.categoria = this.CATEGORIAS.find(c => c === valor) ?? null;
      this.cargar(0);
    });
  }

  async cargar(pagina = this.pagina) {
    this.isLoading = true;
    this.errorMessage = '';
    try {
      // Los usuarios normales solo ven recursos activos (no pueden reservar los inactivos)
      const res = await this.resourceService.list(pagina, this.TAM_PAGINA, this.busqueda, !this.authService.isAdmin(), this.categoria);
      this.recursos = res.content;
      this.pagina = res.number;
      this.totalPaginas = res.totalPages;
      this.totalElementos = res.totalElements;
    } catch (error) {
      this.errorMessage = error instanceof TypeError ? 'No se puede conectar con la API. ¿Está arrancada?' : (error as Error).message;
    } finally {
      this.isLoading = false;
    }
  }

  buscar() {
    this.cargar(0); // al cambiar el filtro volvemos a la primera página
  }

  nuevo() {
    this.editandoId = null;
    this.resourceForm.reset({ name: '', description: '', category: this.categoria ?? 'SALA', capacity: 1 });
    this.formError = '';
    this.formAbierto = true;
  }

  editar(r: Resource) {
    this.editandoId = r.id;
    this.resourceForm.reset({ name: r.name, description: r.description, category: r.category, capacity: r.capacity });
    this.formError = '';
    this.formAbierto = true;
  }

  cancelarForm() {
    this.formAbierto = false;
  }

  async guardar() {
    if (this.resourceForm.invalid) {
      this.resourceForm.markAllAsTouched();
      return;
    }
    this.formError = '';
    try {
      if (this.editandoId === null) {
        await this.resourceService.create(this.resourceForm.value);
      } else {
        await this.resourceService.update(this.editandoId, this.resourceForm.value);
      }
      this.formAbierto = false;
      await this.cargar();
    } catch (error) {
      this.formError = (error as Error).message;
    }
  }

  async desactivar(r: Resource) {
    if (!confirm(`¿Desactivar "${r.name}"? Dejará de poder reservarse.`)) return;
    try {
      await this.resourceService.deactivate(r.id);
      await this.cargar();
    } catch (error) {
      this.errorMessage = (error as Error).message;
    }
  }
}
