import { environment } from '../../../../../environments/environment';
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { timeout } from 'rxjs';
import { PlanEntrenamientoDTO, PlanEntrenamientoCrearDTO, Planes } from '../../../admin/planes';
import {
  AsignacionEntrenadorClienteDTO,
  AsignacionesService
} from '../../../admin/asignaciones';

@Component({
  selector: 'app-mis-planes',
  standalone: false,
  templateUrl: './mis-planes.html',
  styleUrls: ['./mis-planes.scss'],
})
export class MisPlanes implements OnInit {
  private readonly requestTimeoutMs = 15000;

  minFechaPlan: string = new Date().toISOString().split('T')[0];

  planes: PlanEntrenamientoDTO[] = [];
  planesFiltrados: PlanEntrenamientoDTO[] = [];

  cargando = false;
  guardando = false;
  cambiandoEstadoId: string | null = null;
  error: string | null = null;

  mostrarFormularioCrear = false;
  modoEdicion = false;
  idPlanEditando: string | null = null;

  terminoBusqueda = '';
  terminoBusquedaCliente = '';

  entrenadorId = '';
  clientes: any[] = [];
  clienteSeleccionado: any = null;

  clientesAsignados: any[] = [];
  cargandoClientesAsignados = false;

  filtroEstado: 'TODOS' | 'ACTIVOS' | 'INACTIVOS' = 'TODOS';

  paginaActual = 0;
  tamanoPagina = 20;
  totalElementos = 0;
  totalPaginas = 0;
  ultimaPagina = true;
  opcionesTamanoPagina = [10, 20, 50, 100];

  formulario: PlanEntrenamientoCrearDTO = this.crearFormularioVacio();

  constructor(
    private planesService: Planes,
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private asignacionesService: AsignacionesService
  ) {}

  ngOnInit(): void {
    this.inicializarModulo();
  }

  inicializarModulo(): void {
    this.cargando = true;
    this.error = null;

    this.http.get<any>(`${environment.apiUrl}/api/usuarios/me`)
      .pipe(timeout(this.requestTimeoutMs))
      .subscribe({
        next: (res) => {
          this.entrenadorId = res?.id || '';
          this.cargarClientesAsignadosInicial();
          this.cargarPlanes();
        },
        error: (err) => {
          console.error('Error obteniendo usuario actual', err);
          this.error = this.obtenerMensajeError(err, 'No se pudo obtener el usuario actual.');
          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  cargarPlanes(): void {
    this.cargando = true;
    this.error = null;

    this.planesService.getPlanesPaginados(
      this.paginaActual,
      this.tamanoPagina,
      this.terminoBusqueda,
      this.obtenerActivoFiltro()
    )
      .pipe(timeout(this.requestTimeoutMs))
      .subscribe({
        next: (respuesta) => {
          this.planes = this.normalizarPlanes(respuesta.contenido || []);
          this.planesFiltrados = this.planes;

          this.totalElementos = respuesta.totalElementos ?? 0;
          this.totalPaginas = respuesta.totalPaginas ?? 0;
          this.ultimaPagina = respuesta.ultima ?? true;
          this.paginaActual = respuesta.pagina ?? 0;
          this.tamanoPagina = respuesta.tamano ?? this.tamanoPagina;

          this.cargando = false;
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          console.error('Error cargando planes paginados', err);
          this.error = this.obtenerMensajeError(err, 'No se pudieron cargar los planes.');
          this.planes = [];
          this.planesFiltrados = [];

          this.totalElementos = 0;
          this.totalPaginas = 0;
          this.ultimaPagina = true;

          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  aplicarFiltros(): void {
    this.paginaActual = 0;
    this.cargarPlanes();
  }

  obtenerActivoFiltro(): boolean | null {
    if (this.filtroEstado === 'ACTIVOS') {
      return true;
    }

    if (this.filtroEstado === 'INACTIVOS') {
      return false;
    }

    return null;
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

  toggleFormularioCrear(): void {
    if (this.guardando) {
      return;
    }

    if (this.mostrarFormularioCrear || this.modoEdicion) {
      this.cancelarFormulario();
      return;
    }

    this.mostrarFormularioCrear = true;
    this.modoEdicion = false;
    this.error = null;
    this.cdr.detectChanges();
  }

  cargarClientesAsignadosInicial(): void {
    this.cargandoClientesAsignados = true;

    this.asignacionesService.listarMisClientesPaginado(0, 20, '')
      .pipe(timeout(this.requestTimeoutMs))
      .subscribe({
        next: (respuesta) => {
          this.clientesAsignados = (respuesta.contenido || [])
            .filter(asignacion => asignacion.activo)
            .map(asignacion => this.convertirAsignacionACliente(asignacion));

          this.cargandoClientesAsignados = false;
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          console.error('Error cargando clientes asignados', err);
          this.clientesAsignados = [];
          this.cargandoClientesAsignados = false;
          this.cdr.detectChanges();
        }
      });
  }

  buscarClientes(): void {
    const texto = this.terminoBusquedaCliente.trim();

    if (texto.length < 2) {
      this.clientes = [];
      this.cdr.detectChanges();
      return;
    }

    this.asignacionesService.listarMisClientesPaginado(0, 20, texto)
      .pipe(timeout(this.requestTimeoutMs))
      .subscribe({
        next: (respuesta) => {
          this.clientes = (respuesta.contenido || [])
            .filter(asignacion => asignacion.activo)
            .map(asignacion => this.convertirAsignacionACliente(asignacion));

          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error buscando clientes asignados', err);
          this.clientes = [];
          this.cdr.detectChanges();
        }
      });
  }

  convertirAsignacionACliente(asignacion: AsignacionEntrenadorClienteDTO): any {
    return {
      id: asignacion.clienteId,
      nombres: this.obtenerNombresCliente(asignacion.clienteNombre),
      apellidos: this.obtenerApellidosCliente(asignacion.clienteNombre),
      correo: asignacion.clienteCorreo,
      numDoc: asignacion.clienteDocumento,
      activo: asignacion.activo,
      nombreCompleto: asignacion.clienteNombre,
      asignacionId: asignacion.id,
    };
  }

  seleccionarCliente(cliente: any): void {
    this.clienteSeleccionado = cliente;
    this.formulario.clienteId = cliente.id;
    this.terminoBusquedaCliente = this.construirNombreCliente(cliente);
    this.clientes = [];
    this.error = '';
    this.cdr.detectChanges();
  }

  guardarPlan(): void {
    if (this.guardando) {
      return;
    }

    this.error = null;

    if (!this.entrenadorId) {
      this.error = 'No se pudo identificar el entrenador actual.';
      this.cdr.detectChanges();
      return;
    }

    if (!this.formulario.nombre?.trim()) {
      this.error = 'El nombre del plan es obligatorio.';
      this.cdr.detectChanges();
      return;
    }

    if (!this.formulario.descripcion?.trim()) {
      this.error = 'La descripción del plan es obligatoria.';
      this.cdr.detectChanges();
      return;
    }

    if (!this.formulario.objetivo?.trim()) {
      this.error = 'El objetivo del plan es obligatorio.';
      this.cdr.detectChanges();
      return;
    }

    if (!this.formulario.fechaInicio) {
      this.error = 'La fecha de inicio es obligatoria.';
      this.cdr.detectChanges();
      return;
    }

    if (!this.modoEdicion && this.formulario.fechaInicio < this.minFechaPlan) {
      this.error = 'La fecha de inicio no puede ser anterior a hoy.';
      this.cdr.detectChanges();
      return;
    }

    if (!this.formulario.fechaFin) {
      this.error = 'La fecha de fin es obligatoria.';
      this.cdr.detectChanges();
      return;
    }

    if (this.formulario.fechaFin < this.formulario.fechaInicio) {
      this.error = 'La fecha de fin no puede ser anterior a la fecha de inicio.';
      this.cdr.detectChanges();
      return;
    }

    if (!this.formulario.clienteId) {
      this.error = 'Debes seleccionar un cliente.';
      this.cdr.detectChanges();
      return;
    }

    this.formulario.entrenadorId = this.entrenadorId;
    this.guardando = true;
    this.cdr.detectChanges();

    if (this.modoEdicion && this.idPlanEditando) {
      this.planesService.actualizarPlan(this.idPlanEditando, this.formulario)
        .pipe(timeout(this.requestTimeoutMs))
        .subscribe({
          next: () => {
            this.guardando = false;
            this.resetFormulario();
            this.paginaActual = 0;
            this.cargarPlanes();
          },
          error: (err: any) => {
            console.error('Error actualizando plan', err);
            this.error = this.obtenerMensajeError(err, 'No se pudo actualizar el plan.');
            this.guardando = false;
            this.cdr.detectChanges();
          }
        });

      return;
    }

    this.planesService.crearPlan(this.formulario)
      .pipe(timeout(this.requestTimeoutMs))
      .subscribe({
        next: () => {
          this.guardando = false;
          this.resetFormulario();
          this.paginaActual = 0;
          this.cargarPlanes();
        },
        error: (err: any) => {
          console.error('Error creando plan', err);
          this.error = this.obtenerMensajeError(err, 'No se pudo crear el plan.');
          this.guardando = false;
          this.cdr.detectChanges();
        }
      });
  }

  editarPlan(plan: PlanEntrenamientoDTO): void {
    if (this.guardando || this.cambiandoEstadoId) {
      return;
    }

    this.error = null;
    this.modoEdicion = true;
    this.mostrarFormularioCrear = true;
    this.idPlanEditando = plan.id;

    this.formulario = {
      nombre: plan.nombre || '',
      descripcion: plan.descripcion || '',
      objetivo: plan.objetivo || '',
      fechaInicio: plan.fechaInicio || '',
      fechaFin: plan.fechaFin || '',
      clienteId: plan.clienteId || '',
      entrenadorId: plan.entrenadorId || '',
    };

    this.clienteSeleccionado = {
      id: plan.clienteId,
      nombres: plan.clienteNombre || 'Cliente',
      apellidos: '',
      numDoc: ''
    };

    this.terminoBusquedaCliente = plan.clienteNombre || plan.clienteId || '';
    this.clientes = [];

    window.scrollTo({ top: 0, behavior: 'smooth' });
    this.cdr.detectChanges();
  }

  gestionarRutinas(plan: PlanEntrenamientoDTO): void {
    if (!plan?.id) {
      this.error = 'No se pudo identificar el plan seleccionado.';
      this.cdr.detectChanges();
      return;
    }

    this.router.navigate(['/entrenador/MisRutinas', plan.id]);
  }

  cambiarEstado(plan: PlanEntrenamientoDTO): void {
    if (this.cambiandoEstadoId || this.guardando) {
      return;
    }

    const nuevoEstado = !this.normalizarActivo(plan.activo);

    this.error = null;
    this.cambiandoEstadoId = plan.id;
    this.cdr.detectChanges();

    this.planesService.cambiarEstado(plan.id, nuevoEstado)
      .pipe(timeout(this.requestTimeoutMs))
      .subscribe({
        next: () => {
          this.cambiandoEstadoId = null;
          this.cargarPlanes();
        },
        error: (err: any) => {
          console.error('Error cambiando estado del plan', err);
          this.error = this.obtenerMensajeError(err, 'No se pudo cambiar el estado del plan.');
          this.cambiandoEstadoId = null;
          this.cdr.detectChanges();
        }
      });
  }

  cancelarFormulario(): void {
    if (this.guardando) {
      return;
    }

    this.resetFormulario();
    this.error = null;
    this.cdr.detectChanges();
  }

  resetFormulario(): void {
    this.modoEdicion = false;
    this.mostrarFormularioCrear = false;
    this.idPlanEditando = null;

    this.terminoBusquedaCliente = '';
    this.clientes = [];
    this.clienteSeleccionado = null;

    this.formulario = this.crearFormularioVacio();
  }

  volver(): void {
    this.router.navigate(['/entrenador']);
  }

  actualizar(): void {
    if (this.guardando || this.cargando || !!this.cambiandoEstadoId) {
      return;
    }

    this.paginaActual = 0;
    this.cargarPlanes();
  }

  private crearFormularioVacio(): PlanEntrenamientoCrearDTO {
    return {
      nombre: '',
      descripcion: '',
      objetivo: '',
      fechaInicio: '',
      fechaFin: '',
      clienteId: '',
      entrenadorId: '',
    };
  }

  private obtenerMensajeError(err: any, mensajePorDefecto: string): string {
    if (err?.status === 403) {
      return 'No se puede crear el plan. Verifica que estés ingresando como entrenador o que el cliente no tenga ya un plan activo asignado.';
    }

    return (
      err?.error?.mensaje ||
      err?.error?.message ||
      err?.error?.error ||
      err?.message ||
      mensajePorDefecto
    );
  }

  private construirNombreCliente(cliente: any): string {
    const nombreCompleto = `${cliente?.nombres || ''} ${cliente?.apellidos || ''}`.trim();
    const documento = cliente?.numDoc ? ` - ${cliente.numDoc}` : '';
    return `${nombreCompleto}${documento}`.trim();
  }

  private obtenerNombresCliente(nombreCompleto: string | undefined | null): string {
    const partes = (nombreCompleto || '').trim().split(' ').filter(Boolean);

    if (partes.length <= 2) {
      return partes[0] || '';
    }

    return partes.slice(0, 2).join(' ');
  }

  private obtenerApellidosCliente(nombreCompleto: string | undefined | null): string {
    const partes = (nombreCompleto || '').trim().split(' ').filter(Boolean);

    if (partes.length <= 2) {
      return partes.slice(1).join(' ');
    }

    return partes.slice(2).join(' ');
  }

  private normalizarPlanes(data: any): PlanEntrenamientoDTO[] {
    const lista = Array.isArray(data)
      ? data
      : Array.isArray(data?.contenido)
        ? data.contenido
        : Array.isArray(data?.data)
          ? data.data
          : Array.isArray(data?.content)
            ? data.content
            : [];

    return lista.map((plan: any) => ({
      ...plan,
      activo: this.normalizarActivo(plan?.activo),
    }));
  }

  private normalizarActivo(valor: any): boolean {
    if (typeof valor === 'boolean') {
      return valor;
    }

    if (typeof valor === 'string') {
      const normalizado = valor.trim().toLowerCase();
      return normalizado === 'true' || normalizado === 'activo' || normalizado === '1';
    }

    if (typeof valor === 'number') {
      return valor === 1;
    }

    return !!valor;
  }
}