import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';

import { Planes, PlanEntrenamientoDTO } from '../../planes';
import { RutinaDTO, RutinaService } from '../../rutina';

@Component({
  selector: 'app-planes-rutinas',
  standalone: false,
  templateUrl: './planes-rutinas.html',
  styleUrls: ['./planes-rutinas.scss'],
})
export class PlanesRutinas implements OnInit {

  planes: PlanEntrenamientoDTO[] = [];
  planesFiltrados: PlanEntrenamientoDTO[] = [];

  rutinas: RutinaDTO[] = [];
  rutinasFiltradas: RutinaDTO[] = [];

  planSeleccionado: PlanEntrenamientoDTO | null = null;

  cargando = false;
  error = '';

  terminoBusqueda = '';
  filtroEstado = 'TODOS';

  showingRutinas = false;

  paginaActual = 0;
  tamanoPagina = 20;
  totalElementos = 0;
  totalPaginas = 0;
  ultimaPagina = true;
  opcionesTamanoPagina = [10, 20, 50, 100];

  totalPlanes = 0;
  totalActivos = 0;
  totalInactivos = 0;
  clientesConPlan = 0;
  entrenadoresConPlan = 0;

  resumenPorEntrenador: { etiqueta: string; total: number; porcentaje: number }[] = [];
  resumenPorObjetivo: { etiqueta: string; total: number; porcentaje: number }[] = [];

  constructor(
    private planesApi: Planes,
    private rutinaService: RutinaService,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargarPlanes();
  }

  cargarPlanes(): void {
    this.cargando = true;
    this.error = '';
    this.showingRutinas = false;
    this.planSeleccionado = null;
    this.rutinas = [];
    this.rutinasFiltradas = [];
    this.cdr.detectChanges();

    const activoFiltro = this.obtenerActivoFiltro();

    this.planesApi.getPlanesPaginados(
      this.paginaActual,
      this.tamanoPagina,
      this.terminoBusqueda,
      activoFiltro
    ).subscribe({
      next: (respuesta) => {
        this.planes = respuesta.contenido || [];
        this.planesFiltrados = this.planes;

        this.totalElementos = respuesta.totalElementos ?? 0;
        this.totalPaginas = respuesta.totalPaginas ?? 0;
        this.ultimaPagina = respuesta.ultima ?? true;
        this.paginaActual = respuesta.pagina ?? 0;
        this.tamanoPagina = respuesta.tamano ?? this.tamanoPagina;

        this.calcularMetricas();

        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando planes paginados', err);
        this.error = 'Ocurrió un error al cargar los planes de entrenamiento.';
        this.planes = [];
        this.planesFiltrados = [];
        this.totalElementos = 0;
        this.totalPaginas = 0;
        this.ultimaPagina = true;
        this.calcularMetricas();
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  verRutinas(plan: PlanEntrenamientoDTO): void {
    if (!plan?.id) {
      this.error = 'No se pudo identificar el plan seleccionado.';
      return;
    }

    this.planSeleccionado = plan;
    this.showingRutinas = true;
    this.cargando = true;
    this.error = '';
    this.terminoBusqueda = '';
    this.cdr.detectChanges();

    this.rutinaService.getRutinasPorPlan(plan.id).subscribe({
      next: (lista) => {
        this.rutinas = lista || [];
        this.aplicarFiltrosRutinas();
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando rutinas del plan', err);
        this.error = 'No se pudieron cargar las rutinas del plan.';
        this.rutinas = [];
        this.rutinasFiltradas = [];
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  aplicarFiltrosPlanes(): void {
    this.paginaActual = 0;
    this.cargarPlanes();
  }

  aplicarFiltrosRutinas(): void {
    const busqueda = this.normalizarTexto(this.terminoBusqueda);

    this.rutinasFiltradas = this.rutinas.filter(rutina => {
      return !busqueda ||
        this.normalizarTexto(rutina.nombre).includes(busqueda) ||
        this.normalizarTexto(rutina.descripcion).includes(busqueda) ||
        this.normalizarTexto(rutina.diaSemana).includes(busqueda);
    });
  }

  irPaginaAnterior(): void {
    if (this.paginaActual <= 0) {
      return;
    }

    this.paginaActual--;
    this.cargarPlanes();
  }

  irPaginaSiguiente(): void {
    if (this.ultimaPagina || this.paginaActual >= this.totalPaginas - 1) {
      return;
    }

    this.paginaActual++;
    this.cargarPlanes();
  }

  cambiarTamanoPagina(): void {
    this.paginaActual = 0;
    this.cargarPlanes();
  }

  obtenerActivoFiltro(): boolean | null {
    if (this.filtroEstado === 'ACTIVO') {
      return true;
    }

    if (this.filtroEstado === 'INACTIVO') {
      return false;
    }

    return null;
  }

  calcularMetricas(): void {
    const base = this.planesFiltrados.length ? this.planesFiltrados : this.planes;

    this.totalPlanes = this.totalElementos || base.length;
    this.totalActivos = base.filter(p => p.activo).length;
    this.totalInactivos = base.filter(p => !p.activo).length;

    this.clientesConPlan = new Set(
      base
        .filter(p => p.clienteId)
        .map(p => p.clienteId)
    ).size;

    this.entrenadoresConPlan = new Set(
      base
        .filter(p => p.entrenadorId)
        .map(p => p.entrenadorId)
    ).size;

    this.resumenPorEntrenador = this.construirResumenPorCampo(
      base,
      p => p.entrenadorNombre || 'Sin entrenador'
    );

    this.resumenPorObjetivo = this.construirResumenPorCampo(
      base,
      p => p.objetivo || 'Sin objetivo'
    );
  }

  construirResumenPorCampo(
    planes: PlanEntrenamientoDTO[],
    selector: (plan: PlanEntrenamientoDTO) => string
  ): { etiqueta: string; total: number; porcentaje: number }[] {
    const mapa = new Map<string, number>();

    planes.forEach(plan => {
      const clave = selector(plan) || 'Sin dato';
      mapa.set(clave, (mapa.get(clave) || 0) + 1);
    });

    const total = planes.length || 1;

    return Array.from(mapa.entries())
      .map(([clave, cantidad]) => ({
        etiqueta: clave,
        total: cantidad,
        porcentaje: Math.round((cantidad / total) * 100)
      }))
      .sort((a, b) => b.total - a.total)
      .slice(0, 4);
  }

  volverAListaPlanes(): void {
    this.showingRutinas = false;
    this.planSeleccionado = null;
    this.rutinas = [];
    this.rutinasFiltradas = [];
    this.terminoBusqueda = '';
    this.cargarPlanes();
  }

  actualizar(): void {
    if (this.showingRutinas && this.planSeleccionado) {
      this.verRutinas(this.planSeleccionado);
      return;
    }

    this.paginaActual = 0;
    this.cargarPlanes();
  }

  volverAlDashboard(): void {
    this.router.navigate(['/admin']);
  }

  estadoPlan(plan: PlanEntrenamientoDTO): string {
    return plan.activo ? 'ACTIVO' : 'INACTIVO';
  }

  normalizarTexto(texto: string | undefined | null): string {
    return (texto || '')
      .toString()
      .trim()
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');
  }
}