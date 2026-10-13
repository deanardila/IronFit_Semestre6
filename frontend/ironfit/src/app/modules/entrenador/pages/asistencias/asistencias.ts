import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { finalize, timeout } from 'rxjs';
import {
  AsistenciaReporteDTO,
  AsistenciaService,
  FiltrosAsistencia
} from '../../../admin/asistencia';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-asistencias',
  standalone: false,
  templateUrl: './asistencias.html',
  styleUrls: ['./asistencias.scss'],
})
export class Asistencias implements OnInit {

  asistencias: AsistenciaReporteDTO[] = [];
  asistenciasFiltradas: AsistenciaReporteDTO[] = [];

  cargando = false;
  error: string | null = null;

  terminoBusqueda = '';
  filtroEstado = 'TODOS';

  fechaInicio = '';
  fechaFin = '';

  paginaActual = 0;
  tamanoPagina = 20;
  totalElementos = 0;
  totalPaginas = 0;
  ultimaPagina = true;
  opcionesTamanoPagina = [10, 20, 50, 100];

  totalRegistros = 0;
  totalAsistio = 0;
  totalNoAsistio = 0;
  totalSinRutina = 0;
  porcentajeCumplimiento = 0;

  constructor(
    private asistenciaApi: AsistenciaService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const buscar = params['buscar'];

      if (buscar) {
        this.terminoBusqueda = buscar;
      }

      this.paginaActual = 0;
      this.cargarAsistencias();
    });
  }

  cargarAsistencias(): void {
    this.cargando = true;
    this.error = null;
    this.cdr.detectChanges();

    const filtros: FiltrosAsistencia = {
      buscar: this.terminoBusqueda || undefined,
      estado: this.filtroEstado !== 'TODOS' ? this.filtroEstado : undefined,
      fechaInicio: this.fechaInicio || undefined,
      fechaFin: this.fechaFin || undefined,
    };

    this.asistenciaApi.obtenerReporteAsistenciasPaginado(
      this.paginaActual,
      this.tamanoPagina,
      filtros
    )
      .pipe(
        timeout(15000),
        finalize(() => {
          this.cargando = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (respuesta) => {
          this.asistencias = respuesta.contenido || [];
          this.asistenciasFiltradas = this.asistencias;

          this.totalElementos = respuesta.totalElementos ?? 0;
          this.totalPaginas = respuesta.totalPaginas ?? 0;
          this.ultimaPagina = respuesta.ultima ?? true;
          this.paginaActual = respuesta.pagina ?? 0;
          this.tamanoPagina = respuesta.tamano ?? this.tamanoPagina;

          this.calcularMetricas();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando asistencias paginadas', err);

          this.error = 'No se pudieron cargar las asistencias.';
          this.asistencias = [];
          this.asistenciasFiltradas = [];

          this.totalElementos = 0;
          this.totalPaginas = 0;
          this.ultimaPagina = true;

          this.calcularMetricas();
        }
      });
  }

  aplicarFiltros(): void {
    this.paginaActual = 0;
    this.cargarAsistencias();
  }

  limpiarFiltros(): void {
    this.terminoBusqueda = '';
    this.filtroEstado = 'TODOS';
    this.fechaInicio = '';
    this.fechaFin = '';
    this.paginaActual = 0;
    this.cargarAsistencias();
  }

  irPaginaAnterior(): void {
    if (this.paginaActual <= 0) {
      return;
    }

    this.paginaActual--;
    this.cargarAsistencias();
  }

  irPaginaSiguiente(): void {
    if (this.ultimaPagina || this.paginaActual >= this.totalPaginas - 1) {
      return;
    }

    this.paginaActual++;
    this.cargarAsistencias();
  }

  cambiarTamanoPagina(): void {
    this.paginaActual = 0;
    this.cargarAsistencias();
  }

  calcularMetricas(): void {
    this.totalRegistros = this.totalElementos || 0;

    this.totalAsistio = this.asistenciasFiltradas.filter(a =>
      this.normalizarEstado(a.estadoAsistencia) === 'ASISTIO'
    ).length;

    this.totalNoAsistio = this.asistenciasFiltradas.filter(a =>
      this.normalizarEstado(a.estadoAsistencia) === 'NO_ASISTIO'
    ).length;

    this.totalSinRutina = this.asistenciasFiltradas.filter(a =>
      this.normalizarEstado(a.estadoAsistencia) === 'ASISTIO_SIN_RUTINA'
    ).length;

    const totalPagina = this.asistenciasFiltradas.length;

    this.porcentajeCumplimiento = totalPagina > 0
      ? Math.round((this.totalAsistio / totalPagina) * 100)
      : 0;
  }

  claseEstado(estado: string): string {
    const normalizado = this.normalizarEstado(estado);

    if (normalizado === 'ASISTIO') {
      return 'asistio';
    }

    if (normalizado === 'NO_ASISTIO') {
      return 'no-asistio';
    }

    return 'sin-rutina';
  }

  normalizarEstado(estado: string | undefined | null): string {
    return (estado || '')
      .toString()
      .trim()
      .toUpperCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/\s+/g, '_');
  }

  normalizarTexto(texto: string | undefined | null): string {
    return (texto || '')
      .toString()
      .trim()
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');
  }

  actualizar(): void {
    this.paginaActual = 0;
    this.cargarAsistencias();
  }

  volver(): void {
    this.router.navigate(['/entrenador']);
  }
}