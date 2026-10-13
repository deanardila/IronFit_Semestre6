import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { timeout } from 'rxjs';

import {
  EvaluacionFisicaDTO,
  EvaluacionFisicaRequestDTO,
  EvaluacionesFisicasService
} from '../../../admin/evaluaciones-fisicas';

import {
  AsignacionEntrenadorClienteDTO,
  AsignacionesService
} from '../../../admin/asignaciones';

@Component({
  selector: 'app-evaluaciones',
  standalone: false,
  templateUrl: './evaluaciones.html',
  styleUrls: ['./evaluaciones.scss'],
})
export class Evaluaciones implements OnInit {

  private readonly requestTimeoutMs = 15000;

  evaluaciones: EvaluacionFisicaDTO[] = [];
  evaluacionesFiltradas: EvaluacionFisicaDTO[] = [];

  clientesAsignados: any[] = [];
  clientesFiltrados: any[] = [];
  clienteSeleccionado: any = null;

  cargando = false;
  guardando = false;
  eliminandoId: string | null = null;
  cargandoClientes = false;

  error = '';
  mensajeExito = '';

  terminoBusqueda = '';
  terminoBusquedaCliente = '';

  paginaActual = 0;
  tamanoPagina = 20;
  totalElementos = 0;
  totalPaginas = 0;
  ultimaPagina = true;
  opcionesTamanoPagina = [10, 20, 50, 100];

  mostrarFormulario = false;
  modoEdicion = false;
  idEvaluacionEditando: string | null = null;

  formulario: EvaluacionFisicaRequestDTO = this.crearFormularioVacio();

  constructor(
    private router: Router,
    private evaluacionesService: EvaluacionesFisicasService,
    private asignacionesService: AsignacionesService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.error = '';
    this.mensajeExito = '';

    this.cargarClientesAsignados();
    this.cargarEvaluaciones();
  }

  cargarEvaluaciones(): void {
    this.cargando = true;
    this.error = '';

    this.evaluacionesService.listarEvaluacionesPaginadas(
      this.paginaActual,
      this.tamanoPagina,
      this.terminoBusqueda
    )
      .pipe(timeout(this.requestTimeoutMs))
      .subscribe({
        next: (respuesta) => {
          this.evaluaciones = respuesta.contenido || [];
          this.evaluacionesFiltradas = this.evaluaciones;

          this.totalElementos = respuesta.totalElementos ?? 0;
          this.totalPaginas = respuesta.totalPaginas ?? 0;
          this.ultimaPagina = respuesta.ultima ?? true;
          this.paginaActual = respuesta.pagina ?? 0;
          this.tamanoPagina = respuesta.tamano ?? this.tamanoPagina;

          this.cargando = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando evaluaciones físicas paginadas', err);

          this.error = this.obtenerMensajeError(err, 'No se pudieron cargar las evaluaciones físicas.');
          this.evaluaciones = [];
          this.evaluacionesFiltradas = [];

          this.totalElementos = 0;
          this.totalPaginas = 0;
          this.ultimaPagina = true;

          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  cargarClientesAsignados(): void {
    this.cargandoClientes = true;

    this.asignacionesService.listarMisClientesPaginado(0, 100, '')
      .pipe(timeout(this.requestTimeoutMs))
      .subscribe({
        next: (respuesta) => {
          const asignaciones = respuesta.contenido || [];

          this.clientesAsignados = asignaciones
            .filter(a => a.activo)
            .map(a => this.convertirAsignacionACliente(a));

          this.cargandoClientes = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando clientes asignados', err);
          this.clientesAsignados = [];
          this.cargandoClientes = false;
          this.cdr.detectChanges();
        }
      });
  }

  buscarClientes(): void {
    const texto = this.terminoBusquedaCliente.trim();

    if (texto.length < 2) {
      this.clientesFiltrados = [];
      this.cdr.detectChanges();
      return;
    }

    this.asignacionesService.listarMisClientesPaginado(0, 20, texto)
      .pipe(timeout(this.requestTimeoutMs))
      .subscribe({
        next: (respuesta) => {
          this.clientesFiltrados = (respuesta.contenido || [])
            .filter(a => a.activo)
            .map(a => this.convertirAsignacionACliente(a));

          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error buscando clientes asignados', err);
          this.clientesFiltrados = [];
          this.cdr.detectChanges();
        }
      });
  }

  convertirAsignacionACliente(asignacion: AsignacionEntrenadorClienteDTO): any {
    return {
      id: asignacion.clienteId,
      nombreCompleto: asignacion.clienteNombre || 'Cliente sin nombre',
      correo: asignacion.clienteCorreo || '',
      documento: asignacion.clienteDocumento || '',
    };
  }

  aplicarFiltros(): void {
    this.paginaActual = 0;
    this.cargarEvaluaciones();
  }

  irPaginaAnterior(): void {
    if (this.paginaActual <= 0) {
      return;
    }

    this.paginaActual--;
    this.cargarEvaluaciones();
  }

  irPaginaSiguiente(): void {
    if (this.ultimaPagina || this.paginaActual >= this.totalPaginas - 1) {
      return;
    }

    this.paginaActual++;
    this.cargarEvaluaciones();
  }

  cambiarTamanoPagina(): void {
    this.paginaActual = 0;
    this.cargarEvaluaciones();
  }

  seleccionarCliente(cliente: any): void {
    this.clienteSeleccionado = cliente;
    this.formulario.clienteId = cliente.id;
    this.terminoBusquedaCliente = this.construirTextoCliente(cliente);
    this.clientesFiltrados = [];
    this.error = '';
    this.cdr.detectChanges();
  }

  toggleFormulario(): void {
    if (this.guardando) {
      return;
    }

    if (this.mostrarFormulario || this.modoEdicion) {
      this.cancelarFormulario();
      return;
    }

    this.mostrarFormulario = true;
    this.modoEdicion = false;
    this.idEvaluacionEditando = null;
    this.formulario = this.crearFormularioVacio();
    this.clienteSeleccionado = null;
    this.terminoBusquedaCliente = '';
    this.error = '';
    this.mensajeExito = '';
    this.cdr.detectChanges();
  }

  guardarEvaluacion(): void {
    if (this.guardando) {
      return;
    }

    this.error = '';
    this.mensajeExito = '';

    if (!this.formulario.clienteId) {
      this.error = 'Debes seleccionar un cliente asignado.';
      this.cdr.detectChanges();
      return;
    }

    if (!this.formulario.pesoCorporal || this.formulario.pesoCorporal <= 0) {
      this.error = 'El peso corporal debe ser mayor a 0.';
      this.cdr.detectChanges();
      return;
    }

    if (!this.formulario.talla || this.formulario.talla <= 0) {
      this.error = 'La talla debe ser mayor a 0.';
      this.cdr.detectChanges();
      return;
    }

    if (this.formulario.porcentajeGraso != null && this.formulario.porcentajeGraso < 0) {
      this.error = 'El porcentaje graso no puede ser negativo.';
      this.cdr.detectChanges();
      return;
    }

    const request: EvaluacionFisicaRequestDTO = {
      clienteId: this.formulario.clienteId,
      fecha: this.formulario.fecha || null,
      pesoCorporal: Number(this.formulario.pesoCorporal),
      talla: Number(this.formulario.talla),
      porcentajeGraso: this.formulario.porcentajeGraso != null ? Number(this.formulario.porcentajeGraso) : null,
      pecho: this.formulario.pecho != null ? Number(this.formulario.pecho) : null,
      cintura: this.formulario.cintura != null ? Number(this.formulario.cintura) : null,
      brazo: this.formulario.brazo != null ? Number(this.formulario.brazo) : null,
      pierna: this.formulario.pierna != null ? Number(this.formulario.pierna) : null,
      observaciones: this.formulario.observaciones?.trim() || null,
    };

    this.guardando = true;
    this.cdr.detectChanges();

    if (this.modoEdicion && this.idEvaluacionEditando) {
      this.evaluacionesService.actualizarEvaluacion(this.idEvaluacionEditando, request)
        .pipe(timeout(this.requestTimeoutMs))
        .subscribe({
          next: () => {
            this.mensajeExito = 'Evaluación física actualizada correctamente.';
            this.guardando = false;
            this.resetFormulario();
            this.paginaActual = 0;
            this.cargarEvaluaciones();
          },
          error: (err) => {
            console.error('Error actualizando evaluación física', err);
            this.error = this.obtenerMensajeError(err, 'No se pudo actualizar la evaluación física.');
            this.guardando = false;
            this.cdr.detectChanges();
          }
        });

      return;
    }

    this.evaluacionesService.crearEvaluacion(request)
      .pipe(timeout(this.requestTimeoutMs))
      .subscribe({
        next: () => {
          this.mensajeExito = 'Evaluación física creada correctamente.';
          this.guardando = false;
          this.resetFormulario();
          this.paginaActual = 0;
          this.cargarEvaluaciones();
        },
        error: (err) => {
          console.error('Error creando evaluación física', err);
          this.error = this.obtenerMensajeError(err, 'No se pudo crear la evaluación física.');
          this.guardando = false;
          this.cdr.detectChanges();
        }
      });
  }

  editarEvaluacion(evaluacion: EvaluacionFisicaDTO): void {
    if (this.guardando || this.eliminandoId) {
      return;
    }

    this.error = '';
    this.mensajeExito = '';
    this.mostrarFormulario = true;
    this.modoEdicion = true;
    this.idEvaluacionEditando = evaluacion.id;

    this.formulario = {
      clienteId: evaluacion.clienteId,
      fecha: this.formatearFechaParaInput(evaluacion.fecha),
      pesoCorporal: evaluacion.pesoCorporal,
      talla: evaluacion.talla,
      porcentajeGraso: evaluacion.porcentajeGraso ?? null,
      pecho: evaluacion.medidasCorporales?.pecho ?? null,
      cintura: evaluacion.medidasCorporales?.cintura ?? null,
      brazo: evaluacion.medidasCorporales?.brazo ?? null,
      pierna: evaluacion.medidasCorporales?.pierna ?? null,
      observaciones: evaluacion.observaciones || '',
    };

    this.clienteSeleccionado = {
      id: evaluacion.clienteId,
      nombreCompleto: evaluacion.clienteNombre || evaluacion.clienteId,
      documento: '',
      correo: '',
    };

    this.terminoBusquedaCliente = evaluacion.clienteNombre || evaluacion.clienteId;
    this.clientesFiltrados = [];

    window.scrollTo({ top: 0, behavior: 'smooth' });
    this.cdr.detectChanges();
  }

  eliminarEvaluacion(evaluacion: EvaluacionFisicaDTO): void {
    if (this.guardando || this.eliminandoId) {
      return;
    }

    const confirmar = confirm(`¿Seguro que deseas eliminar la evaluación de ${evaluacion.clienteNombre || 'este cliente'}?`);

    if (!confirmar) {
      return;
    }

    this.error = '';
    this.mensajeExito = '';
    this.eliminandoId = evaluacion.id;
    this.cdr.detectChanges();

    this.evaluacionesService.eliminarEvaluacion(evaluacion.id)
      .pipe(timeout(this.requestTimeoutMs))
      .subscribe({
        next: () => {
          this.mensajeExito = 'Evaluación física eliminada correctamente.';
          this.eliminandoId = null;
          this.cargarEvaluaciones();
        },
        error: (err) => {
          console.error('Error eliminando evaluación física', err);
          this.error = this.obtenerMensajeError(err, 'No se pudo eliminar la evaluación física.');
          this.eliminandoId = null;
          this.cdr.detectChanges();
        }
      });
  }

  cancelarFormulario(): void {
    if (this.guardando) {
      return;
    }

    this.resetFormulario();
    this.error = '';
    this.cdr.detectChanges();
  }

  resetFormulario(): void {
    this.mostrarFormulario = false;
    this.modoEdicion = false;
    this.idEvaluacionEditando = null;
    this.formulario = this.crearFormularioVacio();
    this.clienteSeleccionado = null;
    this.terminoBusquedaCliente = '';
    this.clientesFiltrados = [];
  }

  actualizar(): void {
    if (this.cargando || this.guardando || this.eliminandoId) {
      return;
    }

    this.paginaActual = 0;
    this.cargarEvaluaciones();
    this.cargarClientesAsignados();
  }

  volver(): void {
    this.router.navigate(['/entrenador']);
  }

  calcularImcVista(): number | null {
    const peso = Number(this.formulario.pesoCorporal);
    const talla = Number(this.formulario.talla);

    if (!peso || !talla || peso <= 0 || talla <= 0) {
      return null;
    }

    return Math.round((peso / (talla * talla)) * 100) / 100;
  }

  formatearFecha(fecha: string | null | undefined): string {
    if (!fecha) {
      return 'Sin fecha';
    }

    return fecha.replace('T', ' ').substring(0, 16);
  }

  private crearFormularioVacio(): EvaluacionFisicaRequestDTO {
    return {
      clienteId: '',
      fecha: this.obtenerFechaActualInput(),
      pesoCorporal: null,
      talla: null,
      porcentajeGraso: null,
      pecho: null,
      cintura: null,
      brazo: null,
      pierna: null,
      observaciones: '',
    };
  }

  private obtenerFechaActualInput(): string {
    const ahora = new Date();
    ahora.setMinutes(ahora.getMinutes() - ahora.getTimezoneOffset());
    return ahora.toISOString().slice(0, 16);
  }

  private formatearFechaParaInput(fecha: string | null | undefined): string {
    if (!fecha) {
      return this.obtenerFechaActualInput();
    }

    return fecha.substring(0, 16);
  }

  private construirTextoCliente(cliente: any): string {
    const documento = cliente?.documento ? ` - ${cliente.documento}` : '';
    return `${cliente?.nombreCompleto || 'Cliente'}${documento}`;
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