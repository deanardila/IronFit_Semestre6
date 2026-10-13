import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { timeout, finalize } from 'rxjs';

import { Usuarios, UsuarioResumen } from '../../usuarios';
import {
  AsignacionEntrenadorClienteDTO,
  AsignacionesService
} from '../../asignaciones';

@Component({
  selector: 'app-asignaciones',
  standalone: false,
  templateUrl: './asignaciones.html',
  styleUrls: ['./asignaciones.scss'],
})
export class Asignaciones implements OnInit {

  asignaciones: AsignacionEntrenadorClienteDTO[] = [];
  asignacionesFiltradas: AsignacionEntrenadorClienteDTO[] = [];

  usuarios: UsuarioResumen[] = [];
  clientes: UsuarioResumen[] = [];
  entrenadores: UsuarioResumen[] = [];

  cargando = false;
  guardando = false;
  cambiandoEstadoId: string | null = null;

  error = '';
  mensajeExito = '';

  terminoBusqueda = '';
  mostrarFormularioCrear = false;

  nuevaAsignacion = {
    clienteId: '',
    entrenadorId: '',
  };

  constructor(
    private router: Router,
    private usuariosApi: Usuarios,
    private asignacionesApi: AsignacionesService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando = true;
    this.error = '';
    this.mensajeExito = '';

    this.usuariosApi.getUsuariosResumen()
      .pipe(timeout(10000))
      .subscribe({
        next: (usuarios) => {
          this.usuarios = usuarios || [];
          this.clientes = this.usuarios.filter(u => this.tieneRol(u, 'CLIENTE') && u.activo);
          this.entrenadores = this.usuarios.filter(u => this.tieneRol(u, 'ENTRENADOR') && u.activo);

          this.cargarAsignaciones();
        },
        error: (err) => {
          console.error('Error cargando usuarios', err);
          this.error = 'No se pudieron cargar los usuarios.';
          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  cargarAsignaciones(): void {
    this.asignacionesApi.listarAsignaciones()
      .pipe(
        timeout(10000),
        finalize(() => {
          this.cargando = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (asignaciones) => {
          this.asignaciones = asignaciones || [];
          this.aplicarFiltros();
        },
        error: (err) => {
          console.error('Error cargando asignaciones', err);
          this.error = 'No se pudieron cargar las asignaciones.';
          this.asignaciones = [];
          this.asignacionesFiltradas = [];
        }
      });
  }

  aplicarFiltros(): void {
    const busqueda = this.normalizarTexto(this.terminoBusqueda);

    if (!busqueda) {
      this.asignacionesFiltradas = [...this.asignaciones];
      return;
    }

    this.asignacionesFiltradas = this.asignaciones.filter(a => {
      return (
        this.normalizarTexto(a.clienteNombre).includes(busqueda) ||
        this.normalizarTexto(a.clienteDocumento).includes(busqueda) ||
        this.normalizarTexto(a.clienteCorreo).includes(busqueda) ||
        this.normalizarTexto(a.entrenadorNombre).includes(busqueda) ||
        this.normalizarTexto(a.entrenadorCorreo).includes(busqueda)
      );
    });
  }

  toggleFormularioCrear(): void {
    if (this.mostrarFormularioCrear) {
      this.cancelarCrear();
      return;
    }

    this.mostrarFormularioCrear = true;
    this.error = '';
    this.mensajeExito = '';
    this.nuevaAsignacion = {
      clienteId: '',
      entrenadorId: '',
    };

    this.cdr.detectChanges();
  }

  cancelarCrear(): void {
    this.mostrarFormularioCrear = false;
    this.nuevaAsignacion = {
      clienteId: '',
      entrenadorId: '',
    };
    this.cdr.detectChanges();
  }

  guardarAsignacion(): void {
    this.error = '';
    this.mensajeExito = '';

    if (!this.nuevaAsignacion.clienteId) {
      this.error = 'Debes seleccionar un cliente.';
      return;
    }

    if (!this.nuevaAsignacion.entrenadorId) {
      this.error = 'Debes seleccionar un entrenador.';
      return;
    }

    this.guardando = true;

    this.asignacionesApi.crearAsignacion(this.nuevaAsignacion).subscribe({
      next: () => {
        this.mensajeExito = 'Asignación creada correctamente.';
        this.guardando = false;
        this.cancelarCrear();
        this.cargarDatos();
      },
      error: (err) => {
        console.error('Error creando asignación', err);
        this.error = this.obtenerMensajeError(err, 'No se pudo crear la asignación.');
        this.guardando = false;
        this.cdr.detectChanges();
      }
    });
  }

  cambiarEstado(asignacion: AsignacionEntrenadorClienteDTO): void {
    this.error = '';
    this.mensajeExito = '';
    this.cambiandoEstadoId = asignacion.id;

    const nuevoEstado = !asignacion.activo;

    this.asignacionesApi.cambiarEstado(asignacion.id, nuevoEstado).subscribe({
      next: () => {
        this.mensajeExito = nuevoEstado
          ? 'Asignación activada correctamente.'
          : 'Asignación inactivada correctamente.';

        this.cambiandoEstadoId = null;
        this.cargarAsignaciones();
      },
      error: (err) => {
        console.error('Error cambiando estado', err);
        this.error = this.obtenerMensajeError(err, 'No se pudo cambiar el estado de la asignación.');
        this.cambiandoEstadoId = null;
        this.cdr.detectChanges();
      }
    });
  }

  actualizar(): void {
    this.cargarDatos();
  }

  volverAlDashboard(): void {
    this.router.navigate(['/admin']);
  }

  tieneRol(usuario: UsuarioResumen, rol: string): boolean {
    return Array.isArray(usuario.roles)
      && usuario.roles.some(r => String(r).toUpperCase().includes(rol));
  }

  nombreCompleto(usuario: UsuarioResumen): string {
    return `${usuario.nombres || ''} ${usuario.apellidos || ''}`.trim();
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
}