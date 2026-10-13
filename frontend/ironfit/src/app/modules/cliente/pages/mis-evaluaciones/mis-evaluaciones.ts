import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { timeout } from 'rxjs';

import {
  EvaluacionFisicaDTO,
  EvaluacionesFisicasService
} from '../../../admin/evaluaciones-fisicas';

@Component({
  selector: 'app-mis-evaluaciones',
  standalone: false,
  templateUrl: './mis-evaluaciones.html',
  styleUrl: './mis-evaluaciones.scss',
})
export class MisEvaluaciones implements OnInit {

  private readonly requestTimeoutMs = 15000;

  evaluaciones: EvaluacionFisicaDTO[] = [];
  ultimaEvaluacion: EvaluacionFisicaDTO | null = null;
  evaluacionAnterior: EvaluacionFisicaDTO | null = null;

  cargando = false;
  error = '';

  paginaActual = 0;
  tamanoPagina = 5;
  totalElementos = 0;
  totalPaginas = 0;
  ultimaPagina = true;
  opcionesTamanoPagina = [5, 10, 20, 50];

  constructor(
    private router: Router,
    private evaluacionesService: EvaluacionesFisicasService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarMisEvaluaciones();
  }

  cargarMisEvaluaciones(): void {
    this.cargando = true;
    this.error = '';

    this.evaluacionesService.listarEvaluacionesPaginadas(
      this.paginaActual,
      this.tamanoPagina
    )
      .pipe(timeout(this.requestTimeoutMs))
      .subscribe({
        next: (respuesta) => {
          this.evaluaciones = respuesta.contenido || [];

          this.totalElementos = respuesta.totalElementos ?? 0;
          this.totalPaginas = respuesta.totalPaginas ?? 0;
          this.ultimaPagina = respuesta.ultima ?? true;
          this.paginaActual = respuesta.pagina ?? 0;
          this.tamanoPagina = respuesta.tamano ?? this.tamanoPagina;

          this.ultimaEvaluacion = this.evaluaciones.length > 0 ? this.evaluaciones[0] : null;
          this.evaluacionAnterior = this.evaluaciones.length > 1 ? this.evaluaciones[1] : null;

          this.cargando = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando mis evaluaciones físicas paginadas', err);

          this.error = this.obtenerMensajeError(err, 'No se pudieron cargar tus evaluaciones físicas.');
          this.evaluaciones = [];
          this.ultimaEvaluacion = null;
          this.evaluacionAnterior = null;

          this.totalElementos = 0;
          this.totalPaginas = 0;
          this.ultimaPagina = true;

          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  irPaginaAnterior(): void {
    if (this.paginaActual <= 0) {
      return;
    }

    this.paginaActual--;
    this.cargarMisEvaluaciones();
  }

  irPaginaSiguiente(): void {
    if (this.ultimaPagina || this.paginaActual >= this.totalPaginas - 1) {
      return;
    }

    this.paginaActual++;
    this.cargarMisEvaluaciones();
  }

  cambiarTamanoPagina(): void {
    this.paginaActual = 0;
    this.cargarMisEvaluaciones();
  }

  volver(): void {
    this.router.navigate(['/cliente']);
  }

  actualizar(): void {
    if (this.cargando) {
      return;
    }

    this.paginaActual = 0;
    this.cargarMisEvaluaciones();
  }

  calcularDiferenciaPeso(): number | null {
    if (!this.ultimaEvaluacion || !this.evaluacionAnterior) {
      return null;
    }

    return this.redondear(
      this.ultimaEvaluacion.pesoCorporal - this.evaluacionAnterior.pesoCorporal
    );
  }

  calcularDiferenciaGrasa(): number | null {
    if (
      !this.ultimaEvaluacion ||
      !this.evaluacionAnterior ||
      this.ultimaEvaluacion.porcentajeGraso == null ||
      this.evaluacionAnterior.porcentajeGraso == null
    ) {
      return null;
    }

    return this.redondear(
      this.ultimaEvaluacion.porcentajeGraso - this.evaluacionAnterior.porcentajeGraso
    );
  }

  calcularDiferenciaImc(): number | null {
    if (!this.ultimaEvaluacion || !this.evaluacionAnterior) {
      return null;
    }

    return this.redondear(this.ultimaEvaluacion.imc - this.evaluacionAnterior.imc);
  }

  obtenerClaseDiferencia(valor: number | null): string {
    if (valor == null || valor === 0) {
      return 'neutral';
    }

    return valor > 0 ? 'sube' : 'baja';
  }

  formatearDiferencia(valor: number | null, sufijo: string): string {
    if (valor == null) {
      return 'Sin comparación';
    }

    if (valor === 0) {
      return `Sin cambios ${sufijo}`;
    }

    const signo = valor > 0 ? '+' : '';
    return `${signo}${valor} ${sufijo}`;
  }

  obtenerPorcentajeBarra(valor: number | null | undefined, maximo: number): number {
    if (valor == null || valor <= 0) {
      return 0;
    }

    const porcentaje = (valor / maximo) * 100;
    return Math.min(Math.round(porcentaje), 100);
  }

  formatearFecha(fecha: string | null | undefined): string {
    if (!fecha) {
      return 'Sin fecha';
    }

    return fecha.replace('T', ' ').substring(0, 16);
  }

  private redondear(valor: number): number {
    return Math.round(valor * 100) / 100;
  }

  private obtenerMensajeError(err: any, mensajePorDefecto: string): string {
    return (
      err?.error?.mensaje ||
      err?.error?.message ||
      err?.error?.error ||
      err?.error ||
      err?.message ||
      mensajePorDefecto
    );
  }
}