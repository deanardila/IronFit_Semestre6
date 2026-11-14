import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Ejercicios, EjercicioDTO, EjercicioCrearDTO, PaginaResponse } from '../../ejercicios';
import { Router } from '@angular/router';
import { finalize, forkJoin, map, Observable, switchMap, timeout } from 'rxjs';

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
  filtroActivo: boolean | null = null;
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

    const filtroActual = this.filtroActivo;
    const consulta = filtroActual === null
      ? this.obtenerPaginaTodos()
      : this.ejerciciosService.getEjerciciosPaginados(
          this.paginaActual, this.tamanoPagina, this.terminoBusqueda,
          null, null, filtroActual
        ).pipe(map(respuesta => ({
          ...respuesta,
          contenido: (respuesta.contenido ?? []).map(e => ({ ...e, activo: filtroActual }))
        })));

    consulta.pipe(
      timeout(10000),
      finalize(() => {
        this.cargando = false;
        this.cancelarTimeoutCarga();
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (respuesta) => {
        this.ejercicios = respuesta.contenido ?? [];
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

  // El API Mongo pagina por estado; para "Todos" unimos ambos conjuntos
  // respetando los límites de página, sin solicitar ni modificar el backend.
  // Los activos aparecen primero y los inactivos después.
  private obtenerPaginaTodos(): Observable<PaginaResponse<EjercicioDTO>> {
    const size = this.tamanoPagina;
    const page = this.paginaActual;
    const buscar = this.terminoBusqueda;
    const pedir = (pagina: number, estado: boolean, tamano = size) =>
      this.ejerciciosService.getEjerciciosPaginados(
        pagina, tamano, buscar, null, null, estado
      );

    // Consultas ligeras para conocer los totales de cada estado.
    return forkJoin([pedir(0, true, 1), pedir(0, false, 1)]).pipe(
      switchMap(([activos, inactivos]) => {
        const totalActivos = activos.totalElementos ?? 0;
        const totalInactivos = inactivos.totalElementos ?? 0;
        const total = totalActivos + totalInactivos;
        const inicio = page * size;
        const fin = Math.min(inicio + size, total);

        const solicitudes: Observable<EjercicioDTO[]>[] = [];
        const tramo = (estado: boolean, inicioGlobal: number, finGlobal: number, base: number) => {
          const desde = Math.max(0, inicioGlobal - base);
          const hasta = Math.max(0, finGlobal - base);
          if (hasta <= desde) return;
          const primeraPagina = Math.floor(desde / size);
          const ultimaPagina = Math.floor((hasta - 1) / size);
          const paginas = Array.from(
            { length: ultimaPagina - primeraPagina + 1 },
            (_, i) => pedir(primeraPagina + i, estado).pipe(
              map(r => r.contenido ?? [])
            )
          );
          solicitudes.push(forkJoin(paginas).pipe(
            map(resultados => resultados.flat()
              .slice(desde - primeraPagina * size, hasta - primeraPagina * size)
              .map(e => ({ ...e, activo: estado })))
          ));
        };

        tramo(true, inicio, Math.min(fin, totalActivos), 0);
        tramo(false, Math.max(inicio, totalActivos), fin, totalActivos);

        const totalPaginas = Math.ceil(total / size);
        const respuestaBase = {
          pagina: page,
          tamano: size,
          totalElementos: total,
          totalPaginas,
          ultima: page >= totalPaginas - 1
        };
        if (solicitudes.length === 0) {
          return new Observable<PaginaResponse<EjercicioDTO>>(subscriber => {
            subscriber.next({ ...respuestaBase, contenido: [] });
            subscriber.complete();
          });
        }
        return forkJoin(solicitudes).pipe(
          map(resultados => ({ ...respuestaBase, contenido: resultados.flat() }))
        );
      })
    );
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