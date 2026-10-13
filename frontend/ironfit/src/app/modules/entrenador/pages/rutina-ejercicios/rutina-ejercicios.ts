import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import {
  RutinaEjercicioDTO,
  RutinaEjercicioRequest,
  RutinaService
} from '../../../admin/rutina';

import {
  EjercicioDTO,
  Ejercicios
} from '../../../admin/ejercicios';

@Component({
  selector: 'app-rutina-ejercicios',
  standalone: false,
  templateUrl: './rutina-ejercicios.html',
  styleUrl: './rutina-ejercicios.scss',
})
export class RutinaEjercicios implements OnInit {

  rutinaId = '';

  cargando = false;
  cargandoCatalogo = false;
  guardando = false;
  eliminandoId: string | null = null;

  error = '';
  mensajeExito = '';

  ejerciciosRutina: RutinaEjercicioDTO[] = [];
  ejerciciosCatalogo: EjercicioDTO[] = [];
  ejerciciosFiltrados: EjercicioDTO[] = [];

  terminoBusqueda = '';

  paginaCatalogoActual = 0;
  tamanoPaginaCatalogo = 20;
  totalElementosCatalogo = 0;
  totalPaginasCatalogo = 0;
  ultimaPaginaCatalogo = true;
  opcionesTamanoPagina = [10, 20, 50, 100];

  mostrarFormulario = false;
  modoEdicion = false;
  ejercicioEditandoId: string | null = null;

  formulario: RutinaEjercicioRequest = {
    rutinaId: '',
    ejercicioId: '',
    series: 4,
    repeticiones: 12,
    descansoSegundos: 60,
    tiempoSegundos: 0,
    pesoSugerido: 0,
    orden: 1,
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private rutinaService: RutinaService,
    private ejerciciosService: Ejercicios,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('rutinaId');

      if (!id) {
        this.error = 'No se recibió la rutina seleccionada.';
        return;
      }

      this.rutinaId = id;
      this.cargarDatos();
    });
  }

  cargarDatos(): void {
    this.cargarEjerciciosRutina();
  }

  cargarEjerciciosRutina(): void {
    this.cargando = true;
    this.error = '';
    this.mensajeExito = '';

    this.rutinaService.getEjerciciosPorRutina(this.rutinaId).subscribe({
      next: (ejerciciosRutina) => {
        this.ejerciciosRutina = ejerciciosRutina || [];
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando ejercicios de rutina', err);
        this.error = 'No se pudieron cargar los ejercicios de la rutina.';
        this.ejerciciosRutina = [];
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  cargarCatalogoEjercicios(): void {
    this.cargandoCatalogo = true;

    this.ejerciciosService.getEjerciciosPaginados(
      this.paginaCatalogoActual,
      this.tamanoPaginaCatalogo,
      this.terminoBusqueda,
      null,
      null,
      true
    ).subscribe({
      next: (respuesta) => {
        this.ejerciciosCatalogo = respuesta.contenido || [];
        this.ejerciciosFiltrados = this.ejerciciosCatalogo;

        this.totalElementosCatalogo = respuesta.totalElementos ?? 0;
        this.totalPaginasCatalogo = respuesta.totalPaginas ?? 0;
        this.ultimaPaginaCatalogo = respuesta.ultima ?? true;
        this.paginaCatalogoActual = respuesta.pagina ?? 0;
        this.tamanoPaginaCatalogo = respuesta.tamano ?? this.tamanoPaginaCatalogo;

        this.cargandoCatalogo = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando catálogo paginado de ejercicios', err);
        this.error = 'No se pudo cargar el catálogo de ejercicios.';
        this.ejerciciosCatalogo = [];
        this.ejerciciosFiltrados = [];

        this.totalElementosCatalogo = 0;
        this.totalPaginasCatalogo = 0;
        this.ultimaPaginaCatalogo = true;

        this.cargandoCatalogo = false;
        this.cdr.detectChanges();
      }
    });
  }

  aplicarBusqueda(): void {
    this.paginaCatalogoActual = 0;
    this.cargarCatalogoEjercicios();
  }

  irPaginaCatalogoAnterior(): void {
    if (this.paginaCatalogoActual <= 0) {
      return;
    }

    this.paginaCatalogoActual--;
    this.cargarCatalogoEjercicios();
  }

  irPaginaCatalogoSiguiente(): void {
    if (this.ultimaPaginaCatalogo || this.paginaCatalogoActual >= this.totalPaginasCatalogo - 1) {
      return;
    }

    this.paginaCatalogoActual++;
    this.cargarCatalogoEjercicios();
  }

  cambiarTamanoPaginaCatalogo(): void {
    this.paginaCatalogoActual = 0;
    this.cargarCatalogoEjercicios();
  }

  mostrarCrear(): void {
    this.mostrarFormulario = true;
    this.modoEdicion = false;
    this.ejercicioEditandoId = null;
    this.error = '';
    this.mensajeExito = '';

    this.formulario = {
      rutinaId: this.rutinaId,
      ejercicioId: '',
      series: 4,
      repeticiones: 12,
      descansoSegundos: 60,
      tiempoSegundos: 0,
      pesoSugerido: 0,
      orden: this.ejerciciosRutina.length + 1,
    };

    this.paginaCatalogoActual = 0;
    this.cargarCatalogoEjercicios();
  }

  seleccionarEjercicioCatalogo(ejercicio: EjercicioDTO): void {
    this.formulario.ejercicioId = ejercicio.id;

    if (!this.modoEdicion) {
      this.formulario.series = ejercicio.seriesSugeridas || 4;
      this.formulario.repeticiones = ejercicio.repeticionesSugeridas || 12;
    }
  }

  editarEjercicio(item: RutinaEjercicioDTO): void {
    this.mostrarFormulario = true;
    this.modoEdicion = true;
    this.ejercicioEditandoId = item.id;
    this.error = '';
    this.mensajeExito = '';

    this.formulario = {
      rutinaId: item.rutinaId,
      ejercicioId: item.ejercicioId,
      series: item.series || 0,
      repeticiones: item.repeticiones || 0,
      descansoSegundos: item.descansoSegundos || 0,
      tiempoSegundos: item.tiempoSegundos || 0,
      pesoSugerido: item.pesoSugerido || 0,
      orden: item.orden || 1,
    };

    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  guardar(): void {
    this.error = '';
    this.mensajeExito = '';

    if (!this.validarFormulario()) {
      return;
    }

    this.guardando = true;

    if (this.modoEdicion && this.ejercicioEditandoId) {
      const request: RutinaEjercicioRequest = {
        series: this.formulario.series,
        repeticiones: this.formulario.repeticiones,
        descansoSegundos: this.formulario.descansoSegundos,
        tiempoSegundos: this.formulario.tiempoSegundos,
        pesoSugerido: this.formulario.pesoSugerido,
        orden: this.formulario.orden,
      };

      this.rutinaService.actualizarEjercicioRutina(this.ejercicioEditandoId, request).subscribe({
        next: () => {
          this.mensajeExito = 'Ejercicio actualizado correctamente.';
          this.guardando = false;
          this.cancelarFormulario();
          this.cargarEjerciciosRutina();
        },
        error: (err) => {
          console.error('Error actualizando ejercicio de rutina', err);
          this.error = this.obtenerMensajeError(err, 'No se pudo actualizar el ejercicio.');
          this.guardando = false;
          this.cdr.detectChanges();
        }
      });

      return;
    }

    this.rutinaService.crearEjercicioRutina(this.formulario).subscribe({
      next: () => {
        this.mensajeExito = 'Ejercicio agregado correctamente.';
        this.guardando = false;
        this.cancelarFormulario();
        this.cargarEjerciciosRutina();
      },
      error: (err) => {
        console.error('Error agregando ejercicio a rutina', err);
        this.error = this.obtenerMensajeError(err, 'No se pudo agregar el ejercicio.');
        this.guardando = false;
        this.cdr.detectChanges();
      }
    });
  }

  eliminar(item: RutinaEjercicioDTO): void {
    this.error = '';
    this.mensajeExito = '';
    this.eliminandoId = item.id;

    this.rutinaService.eliminarEjercicioRutina(item.id).subscribe({
      next: () => {
        this.mensajeExito = 'Ejercicio eliminado de la rutina.';
        this.eliminandoId = null;
        this.cargarEjerciciosRutina();
      },
      error: (err) => {
        console.error('Error eliminando ejercicio de rutina', err);
        this.error = this.obtenerMensajeError(err, 'No se pudo eliminar el ejercicio.');
        this.eliminandoId = null;
        this.cdr.detectChanges();
      }
    });
  }

  validarFormulario(): boolean {
    if (!this.modoEdicion && !this.formulario.ejercicioId) {
      this.error = 'Debes seleccionar un ejercicio del catálogo.';
      return false;
    }

    if (this.formulario.series == null || this.formulario.series < 0) {
      this.error = 'Las series deben ser mayor o igual a 0.';
      return false;
    }

    if (this.formulario.repeticiones == null || this.formulario.repeticiones < 0) {
      this.error = 'Las repeticiones deben ser mayor o igual a 0.';
      return false;
    }

    if (this.formulario.orden == null || this.formulario.orden < 1) {
      this.error = 'El orden debe ser mayor o igual a 1.';
      return false;
    }

    return true;
  }

  cancelarFormulario(): void {
    this.mostrarFormulario = false;
    this.modoEdicion = false;
    this.ejercicioEditandoId = null;
    this.terminoBusqueda = '';

    this.ejerciciosCatalogo = [];
    this.ejerciciosFiltrados = [];

    this.paginaCatalogoActual = 0;
    this.totalElementosCatalogo = 0;
    this.totalPaginasCatalogo = 0;
    this.ultimaPaginaCatalogo = true;

    this.formulario = {
      rutinaId: this.rutinaId,
      ejercicioId: '',
      series: 4,
      repeticiones: 12,
      descansoSegundos: 60,
      tiempoSegundos: 0,
      pesoSugerido: 0,
      orden: 1,
    };
  }

  volver(): void {
    this.router.navigate(['/entrenador/MisRutinas']);
  }

  actualizar(): void {
    this.cargarEjerciciosRutina();

    if (this.mostrarFormulario && !this.modoEdicion) {
      this.cargarCatalogoEjercicios();
    }
  }

  normalizarTexto(texto: string | undefined | null): string {
    return (texto || '')
      .toString()
      .trim()
      .toUpperCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');
  }

  obtenerMensajeError(err: any, mensajeDefault: string): string {
    return err?.error?.message ||
      err?.error?.mensaje ||
      err?.message ||
      mensajeDefault;
  }
}