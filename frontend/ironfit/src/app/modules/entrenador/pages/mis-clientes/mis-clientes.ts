import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { timeout } from 'rxjs';
import { PlanEntrenamientoDTO, Planes } from '../../../admin/planes';
import { PrediccionPlanDTO, PrediccionesService } from '../../predicciones';
import { IronBotResponse, IronBotService } from '../../../../shared/ironbot';
import {
  AsignacionEntrenadorClienteDTO,
  AsignacionesService
} from '../../../admin/asignaciones';

interface ClienteAsignadoVista {
  id: string;
  nombres: string;
  apellidos: string;
  correo: string;
  telefono: string;
  numDoc: string;
  activo: boolean;
  asignacionId: string;
  entrenadorNombre: string;
}

@Component({
  selector: 'app-mis-clientes',
  standalone: false,
  templateUrl: './mis-clientes.html',
  styleUrls: ['./mis-clientes.scss'],
})
export class MisClientes implements OnInit {

  clientes: ClienteAsignadoVista[] = [];
  clientesFiltrados: ClienteAsignadoVista[] = [];

  clienteDetalleId: string | null = null;
  clienteSeleccionado: ClienteAsignadoVista | null = null;

  identificarCliente(_index: number, cliente: ClienteAsignadoVista): string {
    return cliente.id;
  }

  cerrarCliente(): void {
    this.progresoConsultaId++;
    this.clienteSeleccionado = null;
    this.clienteDetalleId = null;
    this.reiniciarProgreso();
  }
  clienteDetalleTab: 'resumen' | 'planes' | 'progreso' = 'resumen';
  planesDelCliente: PlanEntrenamientoDTO[] = [];
  planProgresoId = '';
  cargandoPlanesProgreso = false;
  errorPlanesProgreso = '';
  prediccion: PrediccionPlanDTO | null = null;
  cargandoPrediccion = false;
  errorPrediccion = '';
  ironBotRespuesta: IronBotResponse | null = null;
  cargandoIronBot = false;
  errorIronBot = '';
  private progresoConsultaId = 0;


  cargando = false;
  error = '';

  nombreUsuario = '';
  terminoBusqueda = '';

  paginaActual = 0;
  tamanoPagina = 20;
  totalElementos = 0;
  totalPaginas = 0;
  ultimaPagina = true;
  opcionesTamanoPagina = [10, 20, 50, 100];

  constructor(
    private router: Router,
    private asignacionesService: AsignacionesService,
    private cdr: ChangeDetectorRef,
    private route: ActivatedRoute,
    private planesService: Planes,
    private prediccionesService: PrediccionesService,
    private ironBotService: IronBotService
  ) {}

  ngOnInit(): void {
    this.nombreUsuario = localStorage.getItem('nombreUsuario') ?? 'Entrenador';

    this.route.queryParams.subscribe(params => {
      const buscar = params['buscar'];

      if (buscar) {
        this.terminoBusqueda = buscar;
      }

      this.paginaActual = 0;
      this.cargarClientes();
    });
  }

  cargarClientes(): void {
    this.cargando = true;
    this.error = '';
    this.cdr.detectChanges();

    this.asignacionesService.listarMisClientesPaginado(
      this.paginaActual,
      this.tamanoPagina,
      this.terminoBusqueda
    ).subscribe({
      next: (respuesta) => {
        this.clientes = (respuesta.contenido || [])
          .filter(asignacion => asignacion.activo)
          .map(asignacion => this.convertirAsignacionACliente(asignacion));

        this.clientesFiltrados = this.clientes;
        if (this.clienteSeleccionado) {
          const actualizado = this.clientes.find(c => c.id === this.clienteSeleccionado?.id);
          this.clienteSeleccionado = actualizado || null;
          this.clienteDetalleId = actualizado?.id || null;
        }

        this.totalElementos = respuesta.totalElementos ?? 0;
        this.totalPaginas = respuesta.totalPaginas ?? 0;
        this.ultimaPagina = respuesta.ultima ?? true;
        this.paginaActual = respuesta.pagina ?? 0;
        this.tamanoPagina = respuesta.tamano ?? this.tamanoPagina;

        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando clientes asignados paginados', err);

        this.error = 'No se pudieron cargar tus clientes asignados.';
        this.clientes = [];
        this.clientesFiltrados = [];

        this.totalElementos = 0;
        this.totalPaginas = 0;
        this.ultimaPagina = true;

        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  convertirAsignacionACliente(asignacion: AsignacionEntrenadorClienteDTO): ClienteAsignadoVista {
    const nombreCompleto = asignacion.clienteNombre || 'Cliente asignado';
    const partes = nombreCompleto.trim().split(' ');

    return {
      id: asignacion.clienteId,
      nombres: partes.slice(0, 2).join(' ') || nombreCompleto,
      apellidos: partes.length > 2 ? partes.slice(2).join(' ') : '',
      correo: asignacion.clienteCorreo || '-',
      telefono: '-',
      numDoc: asignacion.clienteDocumento || '',
      activo: asignacion.activo,
      asignacionId: asignacion.id,
      entrenadorNombre: asignacion.entrenadorNombre || '',
    };
  }

  alternarCliente(cliente: ClienteAsignadoVista): void {
    this.progresoConsultaId++;
    this.reiniciarProgreso();
    this.clienteSeleccionado = cliente;
    this.clienteDetalleId = cliente.id;
    this.clienteDetalleTab = 'resumen';
  }

  abrirProgresoCliente(): void {
    this.clienteDetalleTab = 'progreso';
    if (this.clienteSeleccionado && !this.cargandoPlanesProgreso && this.planesDelCliente.length === 0) {
      this.cargarPlanesProgreso(this.clienteSeleccionado);
    }
  }

  private reiniciarProgreso(): void {
    this.planesDelCliente = [];
    this.planProgresoId = '';
    this.cargandoPlanesProgreso = false;
    this.errorPlanesProgreso = '';
    this.prediccion = null;
    this.cargandoPrediccion = false;
    this.errorPrediccion = '';
    this.ironBotRespuesta = null;
    this.cargandoIronBot = false;
    this.errorIronBot = '';
  }

  cargarPlanesProgreso(cliente: ClienteAsignadoVista): void {
    const consultaId = ++this.progresoConsultaId;
    this.reiniciarProgreso();
    this.cargandoPlanesProgreso = true;
    // La búsqueda se hace por documento (o nombre como respaldo) y la selección
    // final SIEMPRE se restringe al clienteId; nunca se infiere el plan por nombre.
    const buscar = (cliente.numDoc || '').trim() ||
      (cliente.nombres || '').trim().split(/\s+/)[0];
    const resultado: PlanEntrenamientoDTO[] = [];
    const tamano = 100;
    const cargarPagina = (pagina: number): void => {
      this.planesService.getPlanesPaginados(pagina, tamano, buscar, null)
        .pipe(timeout(15000))
        .subscribe({
          next: respuesta => {
            if (consultaId !== this.progresoConsultaId) return;
            resultado.push(...(respuesta.contenido || []).filter(p => p.clienteId === cliente.id));
            if (!respuesta.ultima && pagina + 1 < (respuesta.totalPaginas || 0)) {
              cargarPagina(pagina + 1);
              return;
            }
            this.planesDelCliente = resultado;
            const activo = resultado.find(plan => plan.activo);
            this.planProgresoId = (activo || resultado[0])?.id || '';
            this.cargandoPlanesProgreso = false;
            this.cdr.detectChanges();
          },
          error: () => {
            if (consultaId !== this.progresoConsultaId) return;
            this.errorPlanesProgreso = 'No se pudieron consultar los planes del cliente.';
            this.cargandoPlanesProgreso = false;
            this.cdr.detectChanges();
          }
        });
    };
    cargarPagina(0);
  }

  cambiarPlanProgreso(): void {
    this.progresoConsultaId++;
    this.prediccion = null;
    this.errorPrediccion = '';
    this.cargandoPrediccion = false;
    this.ironBotRespuesta = null;
    this.errorIronBot = '';
    this.cargandoIronBot = false;
  }

  consultarPrediccion(): void {
    const planId = this.planProgresoId;
    if (!planId || this.cargandoPrediccion || this.cargandoIronBot) return;
    const consultaId = ++this.progresoConsultaId;
    this.prediccion = null;
    this.errorPrediccion = '';
    this.ironBotRespuesta = null;
    this.errorIronBot = '';
    this.cargandoPrediccion = true;
    this.prediccionesService.predecirPlan(planId).pipe(timeout(15000)).subscribe({
      next: resultado => {
        if (consultaId !== this.progresoConsultaId) return;
        this.prediccion = resultado;
        this.cargandoPrediccion = false;
        this.cdr.detectChanges();
      },
      error: () => {
        if (consultaId !== this.progresoConsultaId) return;
        this.errorPrediccion = 'No se pudo generar la predicción del plan.';
        this.cargandoPrediccion = false;
        this.cdr.detectChanges();
      }
    });
  }

  interpretarConIronBot(): void {
    const planId = this.planProgresoId;
    if (!planId || !this.prediccion || this.cargandoIronBot) return;
    const consultaId = ++this.progresoConsultaId;
    this.ironBotRespuesta = null;
    this.errorIronBot = '';
    this.cargandoIronBot = true;
    this.ironBotService.analizarPlan(planId).pipe(timeout(45000)).subscribe({
      next: resultado => {
        if (consultaId !== this.progresoConsultaId) return;
        this.ironBotRespuesta = resultado;
        this.cargandoIronBot = false;
        this.cdr.detectChanges();
      },
      error: () => {
        if (consultaId !== this.progresoConsultaId) return;
        this.errorIronBot = 'No se pudo completar la interpretación de IronBot.';
        this.cargandoIronBot = false;
        this.cdr.detectChanges();
      }
    });
  }

  abrirPlanDeCliente(cliente: ClienteAsignadoVista): void {
    // Reutiliza Mis planes, donde Weka e IronBot requieren un plan concreto.
    // No se envía información personal a servicios externos desde esta pantalla.
    // El buscador de planes consulta nombres y apellidos en campos separados.
    // El documento permite encontrar con precisión los planes del cliente.
    const documento = (cliente.numDoc || '').trim();
    const nombre = (cliente.nombres || cliente.apellidos || '').trim().split(/\s+/)[0];
    this.router.navigate(['/entrenador/MisPlanes'], {
      queryParams: { buscar: documento || nombre }
    });
  }

  aplicarFiltros(): void {
    this.paginaActual = 0;
    this.cargarClientes();
  }

  irPaginaAnterior(): void {
    if (this.paginaActual <= 0) {
      return;
    }

    this.paginaActual--;
    this.cargarClientes();
  }

  irPaginaSiguiente(): void {
    if (this.ultimaPagina || this.paginaActual >= this.totalPaginas - 1) {
      return;
    }

    this.paginaActual++;
    this.cargarClientes();
  }

  cambiarTamanoPagina(): void {
    this.paginaActual = 0;
    this.cargarClientes();
  }

  actualizar(): void {
    this.paginaActual = 0;
    this.cargarClientes();
  }

  volverAlDashboard(): void {
    this.router.navigate(['/entrenador']);
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