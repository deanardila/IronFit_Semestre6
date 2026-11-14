import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Ejercicios, EjercicioDTO, EjercicioCrearDTO } from '../../ejercicios';
import { Router } from '@angular/router';
import { finalize, timeout } from 'rxjs';

@Component({
  selector: 'app-gestion-ejercicios',
  standalone: false,
  templateUrl: './gestion-ejercicios.html',
  styleUrls: ['./gestion-ejercicios.scss'],
})
export class GestionEjercicios implements OnInit {
  ejercicios: EjercicioDTO[] = [];
  ejerciciosFiltrados: EjercicioDTO[] = [];
  cargando = false;
  error: string | null = null;
  private timeoutCargaId: ReturnType<typeof setTimeout> | null = null;

  terminoBusqueda = '';

  paginaActual = 0;
  tamanoPagina = 20;
  totalElementos = 0;
  totalPaginas = 0;
  ultimaPagina = true;
  opcionesTamanoPagina = [10, 20, 50, 100];

  mostrarFormularioCrear = false;
  panelAbierto = false;
  pestanaPanel: 'datos' | 'estado' = 'datos';
  filtroActivo = true;
  ejercicioSeleccionado: EjercicioDTO | null = null;
  guardando = false;

  modoEdicion = false;
  idEjercicioEditando: string | null = null;

  formulario: EjercicioCrearDTO = {
    nombre: '',
    descripcion: '',
    categoria: '',
    grupoMuscular: '',
    seriesSugeridas: null,
    repeticionesSugeridas: null,
    tipoEquipo: '',
  };

  constructor(
    private ejerciciosService: Ejercicios,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarEjercicios();
  }

  cargarEjercicios(): void {
    this.cancelarTimeoutCarga();
    this.cargando = true;
    this.error = null;
    this.cdr.detectChanges();

    this.timeoutCargaId = setTimeout(() => {
      if (!this.cargando) {
        return;
      }

      this.cargando = false;
      this.error = 'La carga tardó demasiado. Intenta actualizar nuevamente.';
      this.cdr.detectChanges();
    }, 12000);

    this.ejerciciosService.getEjerciciosPaginados(
      this.paginaActual,
      this.tamanoPagina,
      this.terminoBusqueda,
      null,
      null,
      this.filtroActivo
    ).pipe(
      timeout(10000),
      finalize(() => {
        this.cargando = false;
        this.cancelarTimeoutCarga();
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (respuesta) => {
        this.ejercicios = (respuesta.contenido ?? []).map(e => ({ ...e, activo: this.filtroActivo }));
        this.ejerciciosFiltrados = this.ejercicios;
        if (this.ejercicioSeleccionado) {
          const actual = this.ejercicios.find(e => e.id === this.ejercicioSeleccionado?.id);
          if (actual) this.ejercicioSeleccionado = actual;
        }

        this.totalElementos = respuesta.totalElementos ?? 0;
        this.totalPaginas = respuesta.totalPaginas ?? 0;
        this.ultimaPagina = respuesta.ultima ?? true;
        this.paginaActual = respuesta.pagina ?? 0;
        this.tamanoPagina = respuesta.tamano ?? this.tamanoPagina;

        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando ejercicios paginados', err);

        const status = err?.status;
        const backendMessage = err?.error?.message || err?.message;

        if (status === 0) {
          this.error = 'No hay conexión con el servidor de ejercicios.';
        } else if (status) {
          this.error = `No se pudieron cargar los ejercicios (HTTP ${status}). ${backendMessage || ''}`.trim();
        } else {
          this.error = backendMessage || 'No se pudieron cargar los ejercicios.';
        }

        this.ejercicios = [];
        this.ejerciciosFiltrados = [];

        this.totalElementos = 0;
        this.totalPaginas = 0;
        this.ultimaPagina = true;

        this.cdr.detectChanges();
      },
    });
  }

  aplicarFiltros(): void {
    this.paginaActual = 0;
    this.cargarEjercicios();
  }

  irPaginaAnterior(): void {
    if (this.paginaActual <= 0) {
      return;
    }

    this.paginaActual--;
    this.cargarEjercicios();
  }

  irPaginaSiguiente(): void {
    if (this.ultimaPagina || this.paginaActual >= this.totalPaginas - 1) {
      return;
    }

    this.paginaActual++;
    this.cargarEjercicios();
  }

  cambiarTamanoPagina(): void {
    this.paginaActual = 0;
    this.cargarEjercicios();
  }

  private cancelarTimeoutCarga(): void {
    if (!this.timeoutCargaId) {
      return;
    }

    clearTimeout(this.timeoutCargaId);
    this.timeoutCargaId = null;
  }

  identificarEjercicio(_index: number, ejercicio: EjercicioDTO): string {
    return ejercicio.id;
  }

  cambiarFiltroEstado(): void {
    this.paginaActual = 0;
    this.cargarEjercicios();
  }

  gestionarEjercicio(ejercicio: EjercicioDTO): void {
    this.editarEjercicio(ejercicio);
    this.ejercicioSeleccionado = ejercicio;
    this.pestanaPanel = 'datos';
    this.panelAbierto = true;
  }

  cerrarPanel(): void {
    if (this.guardando) return;
    this.panelAbierto = false;
    this.ejercicioSeleccionado = null;
    this.cancelarCrear();
  }

  guardarEjercicio(): void {
    if (this.guardando) return;
    this.error = null;
    this.guardando = true;

    if (this.modoEdicion && this.idEjercicioEditando) {
      this.ejerciciosService.actualizarEjercicio(this.idEjercicioEditando, this.formulario).subscribe({
        next: (actualizado) => {
          const id = this.idEjercicioEditando;
          this.ejercicios = this.ejercicios.map(e =>
            e.id === id ? { ...actualizado, activo: e.activo } : e
          );
          this.ejerciciosFiltrados = this.ejercicios;
          this.guardando = false;
          this.cerrarPanel();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error actualizando ejercicio', err);
          this.error = err?.error?.message || 'No se pudo actualizar el ejercicio.';
          this.guardando = false;
          this.cdr.detectChanges();
        }
      });
      return;
    }

    this.ejerciciosService.crearEjercicio(this.formulario).subscribe({
      next: () => {
        this.guardando = false;
        this.cerrarPanel();
        this.filtroActivo = true;
        this.paginaActual = 0;
        this.cargarEjercicios();
      },
      error: (err) => {
        console.error('Error creando ejercicio', err);
        this.error = err?.error?.message || 'No se pudo crear el ejercicio.';
        this.guardando = false;
        this.cdr.detectChanges();
      }
    });
  }

  editarEjercicio(ejercicio: EjercicioDTO): void {
    this.mostrarFormularioCrear = true;
    this.modoEdicion = true;
    this.idEjercicioEditando = ejercicio.id;
    this.formulario = {
      nombre: ejercicio.nombre,
      descripcion: ejercicio.descripcion,
      categoria: ejercicio.categoria,
      grupoMuscular: ejercicio.grupoMuscular,
      seriesSugeridas: ejercicio.seriesSugeridas,
      repeticionesSugeridas: ejercicio.repeticionesSugeridas,
      tipoEquipo: ejercicio.tipoEquipo || '',
    };
  }

  cambiarEstadoEjercicio(): void {
    const ejercicio = this.ejercicioSeleccionado;
    if (!ejercicio || this.guardando) return;
    const nuevoEstado = ejercicio.activo === false;
    if (!window.confirm(`¿Deseas ${nuevoEstado ? 'activar' : 'inactivar'} el ejercicio "${ejercicio.nombre}"?`)) return;

    this.guardando = true;
    this.ejerciciosService.cambiarEstado(ejercicio.id, nuevoEstado).subscribe({
      next: () => {
        // Conservar la fila actual mientras se administra, sin saltar al inicio.
        ejercicio.activo = nuevoEstado;
        this.ejercicios = this.ejercicios.map(e => e.id === ejercicio.id ? { ...e, activo: nuevoEstado } : e);
        this.ejerciciosFiltrados = this.ejercicios;
        this.ejercicioSeleccionado = this.ejercicios.find(e => e.id === ejercicio.id) || ejercicio;
        this.guardando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cambiando estado del ejercicio', err);
        this.error = err?.error?.message || 'No se pudo cambiar el estado del ejercicio.';
        this.guardando = false;
        this.cdr.detectChanges();
      }
    });
  }

  eliminarEjercicio(id: string): void {
    const ejercicio = this.ejercicios.find(e => e.id === id);
    if (ejercicio) {
      this.ejercicioSeleccionado = ejercicio;
      this.cambiarEstadoEjercicio();
    }
  }

  resetFormulario(): void {
    this.modoEdicion = false;
    this.idEjercicioEditando = null;

    this.formulario = {
      nombre: '',
      descripcion: '',
      categoria: '',
      grupoMuscular: '',
      seriesSugeridas: null,
      repeticionesSugeridas: null,
      tipoEquipo: '',
    };
  }

  abrirFormularioCrear(): void {
    this.resetFormulario();
    this.mostrarFormularioCrear = true;
    this.ejercicioSeleccionado = null;
    this.pestanaPanel = 'datos';
    this.panelAbierto = true;
    this.cdr.detectChanges();
  }

  toggleFormularioCrear(forzarApertura = false): void {
    if (this.panelAbierto && !forzarApertura) {
      this.cerrarPanel();
    } else {
      this.abrirFormularioCrear();
    }
  }

  cancelarCrear(): void {
    this.mostrarFormularioCrear = false;
    this.resetFormulario();
    this.cdr.detectChanges();
  }

  actualizar(): void {
    this.cargarEjercicios();
  }

  volver(): void {
    this.router.navigate(['/admin']);
  }
}