import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
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
    private route: ActivatedRoute
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