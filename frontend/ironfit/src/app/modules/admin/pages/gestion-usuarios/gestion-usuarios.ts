import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Usuarios, UsuarioResumen } from '../../usuarios';
import { finalize, timeout } from 'rxjs';

import {
  AsignacionEntrenadorClienteDTO,
  AsignacionesService
} from '../../asignaciones';

@Component({
  selector: 'app-gestion-usuarios',
  standalone: false,
  templateUrl: './gestion-usuarios.html',
  styleUrls: ['./gestion-usuarios.scss'],
})
export class GestionUsuarios implements OnInit {

  usuarios: UsuarioResumen[] = [];
  usuariosFiltrados: UsuarioResumen[] = [];

  asignaciones: AsignacionEntrenadorClienteDTO[] = [];
  entrenadoresActivos: UsuarioResumen[] = [];

  filtroTipo: string | null = null;
  filtroEstado: string | null = null;

  terminoBusqueda = '';

  paginaActual = 0;
  tamanoPagina = 20;
  totalElementos = 0;
  totalPaginas = 0;
  ultimaPagina = true;

  opcionesTamanoPagina = [10, 20, 50, 100];

  cargando = false;
  vieneDeDashboard = false;

  mostrarFormularioCrear = false;

  // Asignar / cambiar entrenador
  mostrarFormularioAsignacion = false;
  clienteSeleccionadoAsignacion: UsuarioResumen | null = null;
  entrenadorSeleccionadoId = '';
  guardandoAsignacion = false;

  nuevoUsuario: any = {
    tipoDoc: 'CC',
    numDoc: '',
    nombres: '',
    apellidos: '',
    correo: '',
    telefono: '',
    activo: true,
    rol: 'CLIENTE'
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private usuariosApi: Usuarios,
    private asignacionesApi: AsignacionesService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['tipo'] || params['estado']) {
        this.vieneDeDashboard = true;
        this.filtroTipo = params['tipo'] ?? null;
        this.filtroEstado = params['estado'] ?? null;
      } else {
        this.vieneDeDashboard = false;
        this.filtroTipo = null;
        this.filtroEstado = null;
      }

      this.cargarUsuarios();
    });
  }

  cargarUsuarios(): void {
    this.cargando = true;
    this.cdr.detectChanges();

    const rolFiltro = this.filtroTipo ? this.filtroTipo.toUpperCase() : null;
    const activoFiltro = this.filtroEstado
      ? this.filtroEstado === 'ACTIVO'
      : null;

    this.usuariosApi.getUsuariosPaginados(
      this.paginaActual,
      this.tamanoPagina,
      this.terminoBusqueda,
      rolFiltro,
      activoFiltro
    ).pipe(
      timeout(10000),
      finalize(() => {
        this.cargando = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (respuesta) => {
        this.usuarios = respuesta.contenido ?? [];
        this.usuariosFiltrados = this.usuarios;

        this.totalElementos = respuesta.totalElementos ?? 0;
        this.totalPaginas = respuesta.totalPaginas ?? 0;
        this.ultimaPagina = respuesta.ultima ?? true;
        this.paginaActual = respuesta.pagina ?? 0;
        this.tamanoPagina = respuesta.tamano ?? this.tamanoPagina;

        this.cargarEntrenadoresActivos();
        this.cargarAsignaciones();
      },
      error: (err) => {
        console.error('Error cargando usuarios paginados', err);
        this.usuarios = [];
        this.usuariosFiltrados = [];
        this.asignaciones = [];

        this.totalElementos = 0;
        this.totalPaginas = 0;
        this.ultimaPagina = true;

        this.cdr.detectChanges();
      }
    });
  }

  cargarEntrenadoresActivos(): void {
    this.usuariosApi.getUsuariosPaginados(
      0,
      100,
      '',
      'ENTRENADOR',
      true
    ).pipe(
      timeout(10000)
    ).subscribe({
      next: (respuesta) => {
        this.entrenadoresActivos = respuesta.contenido ?? [];
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando entrenadores activos', err);
        this.entrenadoresActivos = [];
        this.cdr.detectChanges();
      }
    });
  }

  cargarAsignaciones(): void {
    this.asignacionesApi.listarAsignaciones().pipe(
      timeout(10000)
    ).subscribe({
      next: (asignaciones) => {
        this.asignaciones = asignaciones || [];
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando asignaciones', err);
        this.asignaciones = [];
        this.cdr.detectChanges();
      }
    });
  }

  aplicarFiltros(): void {
    this.paginaActual = 0;
    this.cargarUsuarios();
  }

  irPaginaAnterior(): void {
    if (this.paginaActual <= 0) {
      return;
    }

    this.paginaActual--;
    this.cargarUsuarios();
  }

  irPaginaSiguiente(): void {
    if (this.ultimaPagina || this.paginaActual >= this.totalPaginas - 1) {
      return;
    }

    this.paginaActual++;
    this.cargarUsuarios();
  }

  cambiarTamanoPagina(): void {
    this.paginaActual = 0;
    this.cargarUsuarios();
  }

  abrirFormularioCrear(): void {
    this.toggleFormularioCrear(true);
  }

  toggleFormularioCrear(forzarApertura = false): void {
    const debeAbrir = forzarApertura || !this.mostrarFormularioCrear;

    if (!debeAbrir) {
      this.cancelarCrear();
      return;
    }

    this.mostrarFormularioCrear = true;
    this.mostrarFormularioAsignacion = false;

    this.nuevoUsuario = {
      tipoDoc: 'CC',
      numDoc: '',
      nombres: '',
      apellidos: '',
      correo: '',
      telefono: '',
      activo: true,
      rol: 'CLIENTE'
    };

    this.cdr.detectChanges();
  }

  cancelarCrear(): void {
    this.mostrarFormularioCrear = false;
    this.cdr.detectChanges();
  }

  guardarUsuario(): void {
    const payload = {
      tipoDoc: this.nuevoUsuario.tipoDoc,
      numDoc: this.nuevoUsuario.numDoc,
      nombres: this.nuevoUsuario.nombres,
      apellidos: this.nuevoUsuario.apellidos,
      correo: this.nuevoUsuario.correo,
      telefono: this.nuevoUsuario.telefono,
      password: 'Ironfit123*',
      roles: [this.nuevoUsuario.rol]
    };

    this.usuariosApi.crearUsuario(payload).subscribe({
      next: () => {
        this.mostrarFormularioCrear = false;
        this.paginaActual = 0;
        this.cargarUsuarios();
      },
      error: (err) => {
        console.error('Error creando usuario', err);
      }
    });
  }

  toggleEstado(usuario: UsuarioResumen): void {
    const nuevoEstado = !usuario.activo;

    this.usuariosApi.cambiarEstado(usuario.id, nuevoEstado).subscribe({
      next: () => {
        this.cargarUsuarios();
      },
      error: (err) => {
        console.error('Error cambiando estado del usuario', err);
      }
    });
  }

  resetearPassword(usuario: UsuarioResumen): void {
    const confirmar = confirm(
      `¿Resetear la contraseña de ${usuario.nombres} ${usuario.apellidos} a la clave general?`
    );

    if (!confirmar) return;

    this.usuariosApi.resetearPassword(usuario.id).subscribe({
      next: () => {
        alert('Contraseña restablecida a: Ironfit123*');
      },
      error: (err) => {
        console.error('Error reseteando contraseña', err);
      }
    });
  }

  // ================== ASIGNACIONES ==================

  abrirFormularioAsignacion(cliente: UsuarioResumen): void {
    this.clienteSeleccionadoAsignacion = cliente;

    const asignacionActual = this.obtenerAsignacionActiva(cliente.id);

    this.entrenadorSeleccionadoId = asignacionActual?.entrenadorId || '';
    this.mostrarFormularioAsignacion = true;
    this.mostrarFormularioCrear = false;

    this.cdr.detectChanges();
  }

  cancelarAsignacion(): void {
    this.mostrarFormularioAsignacion = false;
    this.clienteSeleccionadoAsignacion = null;
    this.entrenadorSeleccionadoId = '';
    this.guardandoAsignacion = false;
    this.cdr.detectChanges();
  }

  guardarAsignacionEntrenador(): void {
    if (!this.clienteSeleccionadoAsignacion) {
      return;
    }

    if (!this.entrenadorSeleccionadoId) {
      alert('Selecciona un entrenador.');
      return;
    }

    const cliente = this.clienteSeleccionadoAsignacion;
    const asignacionActual = this.obtenerAsignacionActiva(cliente.id);

    if (asignacionActual && asignacionActual.entrenadorId === this.entrenadorSeleccionadoId) {
      alert('Este cliente ya está asignado a ese entrenador.');
      return;
    }

    this.guardandoAsignacion = true;

    const crearNueva = () => {
      this.asignacionesApi.crearAsignacion({
        clienteId: cliente.id,
        entrenadorId: this.entrenadorSeleccionadoId
      }).subscribe({
        next: () => {
          this.guardandoAsignacion = false;
          this.cancelarAsignacion();
          this.cargarUsuarios();
        },
        error: (err) => {
          console.error('Error creando asignación', err);
          this.guardandoAsignacion = false;
          alert(this.obtenerMensajeError(err, 'No se pudo crear la asignación.'));
          this.cdr.detectChanges();
        }
      });
    };

    if (asignacionActual) {
      this.asignacionesApi.cambiarEstado(asignacionActual.id, false).subscribe({
        next: () => crearNueva(),
        error: (err) => {
          console.error('Error inactivando asignación anterior', err);
          this.guardandoAsignacion = false;
          alert(this.obtenerMensajeError(err, 'No se pudo inactivar la asignación anterior.'));
          this.cdr.detectChanges();
        }
      });

      return;
    }

    crearNueva();
  }

  obtenerAsignacionActiva(clienteId: string): AsignacionEntrenadorClienteDTO | undefined {
    return this.asignaciones.find(
      a => a.clienteId === clienteId && a.activo
    );
  }

  textoEntrenadorAsignado(usuario: UsuarioResumen): string {
    if (!this.esCliente(usuario)) {
      return 'No aplica';
    }

    const asignacion = this.obtenerAsignacionActiva(usuario.id);

    return asignacion?.entrenadorNombre || 'Sin asignar';
  }

  textoBotonAsignacion(usuario: UsuarioResumen): string {
    return this.obtenerAsignacionActiva(usuario.id) ? 'Cambiar' : 'Asignar';
  }

  // ================== HELPERS ==================

  esCliente(usuario: UsuarioResumen): boolean {
    return Array.isArray(usuario.roles) &&
      usuario.roles.some(r => String(r).toUpperCase().includes('CLIENTE'));
  }

  esEntrenador(usuario: UsuarioResumen): boolean {
    return Array.isArray(usuario.roles) &&
      usuario.roles.some(r => String(r).toUpperCase().includes('ENTRENADOR'));
  }

  normalizarTexto(texto: string | undefined | null): string {
    return (texto || '')
      .toString()
      .trim()
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');
  }

  obtenerMensajeError(err: any, mensajeDefault: string): string {
    return err?.error?.message ||
      err?.error?.mensaje ||
      err?.error ||
      err?.message ||
      mensajeDefault;
  }

  actualizar(): void {
    this.paginaActual = 0;
    this.cargarUsuarios();
  }

  volverAlDashboard(): void {
    this.router.navigate(['/admin']);
  }
}