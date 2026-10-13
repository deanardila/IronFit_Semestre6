import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { finalize, timeout } from 'rxjs';
import { Router } from '@angular/router';

import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

import {
  AsistenciaReporteDTO,
  AsistenciaService,
  FiltrosAsistencia
} from '../../asistencia';

@Component({
  selector: 'app-auditoria-reportes',
  standalone: false,
  templateUrl: './auditoria-reportes.html',
  styleUrls: ['./auditoria-reportes.scss'],
})
export class AuditoriaReportes implements OnInit {

  resumenes: AsistenciaReporteDTO[] = [];
  resumenesFiltrados: AsistenciaReporteDTO[] = [];

  cargando = false;
  error = '';

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
  porcentajeAsistencia = 0;

  constructor(
    private asistenciaService: AsistenciaService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarResumen();
  }

  cargarResumen(): void {
    this.cargando = true;
    this.error = '';
    this.cdr.detectChanges();

    const filtros: FiltrosAsistencia = {
      buscar: this.terminoBusqueda || undefined,
      fechaInicio: this.fechaInicio || undefined,
      fechaFin: this.fechaFin || undefined,
      estado: this.filtroEstado !== 'TODOS' ? this.filtroEstado : undefined,
    };

    this.asistenciaService.obtenerReporteAsistenciasPaginado(
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
          this.resumenes = respuesta.contenido ?? [];
          this.resumenesFiltrados = this.resumenes;

          this.totalElementos = respuesta.totalElementos ?? 0;
          this.totalPaginas = respuesta.totalPaginas ?? 0;
          this.ultimaPagina = respuesta.ultima ?? true;
          this.paginaActual = respuesta.pagina ?? 0;
          this.tamanoPagina = respuesta.tamano ?? this.tamanoPagina;

          this.calcularMetricas();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando reportes de asistencia paginados', err);

          this.error = 'Ocurrió un error al cargar las asistencias.';
          this.resumenes = [];
          this.resumenesFiltrados = [];

          this.totalElementos = 0;
          this.totalPaginas = 0;
          this.ultimaPagina = true;

          this.calcularMetricas();
          this.cdr.detectChanges();
        }
      });
  }

  aplicarFiltros(): void {
    this.paginaActual = 0;
    this.cargarResumen();
  }

  irPaginaAnterior(): void {
    if (this.paginaActual <= 0) {
      return;
    }

    this.paginaActual--;
    this.cargarResumen();
  }

  irPaginaSiguiente(): void {
    if (this.ultimaPagina || this.paginaActual >= this.totalPaginas - 1) {
      return;
    }

    this.paginaActual++;
    this.cargarResumen();
  }

  cambiarTamanoPagina(): void {
    this.paginaActual = 0;
    this.cargarResumen();
  }

  calcularMetricas(): void {
    this.totalRegistros = this.totalElementos || this.resumenesFiltrados.length;

    this.totalAsistio = this.resumenesFiltrados.filter(r =>
      this.normalizarEstado(r.estadoAsistencia) === 'ASISTIO'
    ).length;

    this.totalNoAsistio = this.resumenesFiltrados.filter(r =>
      this.normalizarEstado(r.estadoAsistencia) === 'NO_ASISTIO'
    ).length;

    this.totalSinRutina = this.resumenesFiltrados.filter(r =>
      this.normalizarEstado(r.estadoAsistencia) === 'ASISTIO_SIN_RUTINA'
    ).length;

    const registrosPagina = this.resumenesFiltrados.length;

    this.porcentajeAsistencia = registrosPagina > 0
      ? Math.round((this.totalAsistio / registrosPagina) * 100)
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

  limpiarFiltros(): void {
    this.terminoBusqueda = '';
    this.filtroEstado = 'TODOS';
    this.fechaInicio = '';
    this.fechaFin = '';
    this.paginaActual = 0;
    this.cargarResumen();
  }

  actualizar(): void {
    this.paginaActual = 0;
    this.cargarResumen();
  }

  exportarPDF(): void {
    if (this.resumenesFiltrados.length === 0) {
      alert('No hay registros para exportar.');
      return;
    }

    const doc = new jsPDF({
      orientation: 'landscape',
      unit: 'mm',
      format: 'a4'
    });

    const fechaGeneracion = new Date().toLocaleString('es-CO');

    doc.setFontSize(16);
    doc.text('IRONFIT - Reporte de Asistencias', 14, 15);

    doc.setFontSize(10);
    doc.text(`Fecha de generación: ${fechaGeneracion}`, 14, 22);

    const rango = this.fechaInicio || this.fechaFin
      ? `Rango: ${this.fechaInicio || 'Inicio'} / ${this.fechaFin || 'Hoy'}`
      : 'Rango: Todos los registros';

    doc.text(rango, 14, 28);

    doc.text(`Página exportada: ${this.paginaActual + 1} de ${this.totalPaginas || 1}`, 14, 36);
    doc.text(`Total general: ${this.totalElementos}`, 70, 36);
    doc.text(`Registros en página: ${this.resumenesFiltrados.length}`, 120, 36);
    doc.text(`Cumplimiento página: ${this.porcentajeAsistencia}%`, 190, 36);

    const filas = this.resumenesFiltrados.map(r => [
      r.fechaAsistencia || '-',
      r.clienteNombre || '-',
      r.clienteDocumento || '-',
      r.entrenadorNombre || '-',
      r.planNombre || '-',
      r.rutinaNombre || '-',
      r.estadoAsistencia || '-',
      r.observaciones || '-'
    ]);

    autoTable(doc, {
      startY: 44,
      head: [[
        'Fecha',
        'Cliente',
        'Documento',
        'Entrenador',
        'Plan',
        'Rutina',
        'Estado',
        'Observaciones'
      ]],
      body: filas,
      styles: {
        fontSize: 8,
        cellPadding: 2
      },
      headStyles: {
        fillColor: [255, 138, 31],
        textColor: [20, 20, 20]
      },
      alternateRowStyles: {
        fillColor: [245, 245, 245]
      }
    });

    doc.save('reporte-asistencias-ironfit.pdf');
  }

  volverAlDashboard(): void {
    this.router.navigate(['/admin']);
  }
}