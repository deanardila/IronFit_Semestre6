import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PlanEntrenamientoDTO, Planes } from '../../../admin/planes';
import { RutinaDTO, RutinaEjercicioDTO, RutinaService } from '../../../admin/rutina';
import {
  EntrenamientoRealizadoCrearRequest,
  EntrenamientoRealizadoService
} from '../../../admin/entrenamiento-realizado';

interface DatosEjercicioSesion {
  ejercicioId: string;
  ejercicioNombre: string;
  seriesRealizadas: number;
  repeticionesRealizadas: number;
  pesoUsado: number;
  completado: boolean;
}

@Component({
  selector: 'app-mis-rutinas',
  standalone: false,
  templateUrl: './mis-rutinas.html',
  styleUrl: './mis-rutinas.scss',
})
export class MisRutinas implements OnInit {

  cargando = false;
  cargandoEjerciciosId: string | null = null;
  guardandoEntrenamiento = false;

  error = '';
  mensajeExito = '';

  planes: PlanEntrenamientoDTO[] = [];
  planActivo: PlanEntrenamientoDTO | null = null;

  rutinas: RutinaDTO[] = [];
  rutinasFiltradas: RutinaDTO[] = [];

  diaSeleccionado = 'TODOS';

  ejerciciosPorRutina: { [rutinaId: string]: RutinaEjercicioDTO[] } = {};
  rutinaAbiertaId: string | null = null;

  modoSesionActiva = false;
  rutinaSesion: RutinaDTO | null = null;
  ejerciciosSesion: RutinaEjercicioDTO[] = [];
  datosSesion: { [ejercicioId: string]: DatosEjercicioSesion } = {};
  observacionCliente = '';

  dias = [
    { valor: 'TODOS', etiqueta: 'Todos' },
    { valor: 'LUNES', etiqueta: 'Lunes' },
    { valor: 'MARTES', etiqueta: 'Martes' },
    { valor: 'MIERCOLES', etiqueta: 'Miércoles' },
    { valor: 'JUEVES', etiqueta: 'Jueves' },
    { valor: 'VIERNES', etiqueta: 'Viernes' },
    { valor: 'SABADO', etiqueta: 'Sábado' },
    { valor: 'DOMINGO', etiqueta: 'Domingo' },
  ];

  constructor(
    private planesService: Planes,
    private rutinaService: RutinaService,
    private entrenamientoRealizadoService: EntrenamientoRealizadoService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarMisRutinas();
  }

  cargarMisRutinas(): void {
    this.cargando = true;
    this.error = '';
    this.mensajeExito = '';
    this.rutinas = [];
    this.rutinasFiltradas = [];
    this.planActivo = null;
    this.rutinaAbiertaId = null;
    this.ejerciciosPorRutina = {};
    this.cancelarSesion();

    this.planesService.getPlanesPaginados(0, 1, '', true).subscribe({
      next: (respuesta) => {
        this.planes = respuesta.contenido || [];
        this.planActivo = this.planes.length > 0 ? this.planes[0] : null;

        if (!this.planActivo) {
          this.cargando = false;
          this.error = '';
          this.cdr.detectChanges();
          return;
        }

        this.cargarRutinasDelPlan(this.planActivo.id);
      },
      error: (err) => {
        console.error('Error cargando plan activo del cliente', err);
        this.error = this.obtenerMensajeError(err, 'No se pudo cargar tu plan de entrenamiento.');
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  cargarRutinasDelPlan(planId: string): void {
    this.rutinaService.getRutinasPorPlan(planId).subscribe({
      next: (rutinas) => {
        this.rutinas = (rutinas || []).sort((a, b) => (a.orden || 0) - (b.orden || 0));
        this.aplicarFiltroDia();
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando rutinas', err);
        this.error = this.obtenerMensajeError(err, 'No se pudieron cargar tus rutinas.');
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  seleccionarDia(dia: string): void {
    this.diaSeleccionado = dia;
    this.aplicarFiltroDia();
  }

  aplicarFiltroDia(): void {
    if (this.diaSeleccionado === 'TODOS') {
      this.rutinasFiltradas = [...this.rutinas];
    } else {
      this.rutinasFiltradas = this.rutinas.filter(rutina =>
        this.normalizarTexto(rutina.diaSemana) === this.diaSeleccionado
      );
    }
  }

  toggleRutina(rutina: RutinaDTO): void {
    if (this.rutinaAbiertaId === rutina.id) {
      this.rutinaAbiertaId = null;
      return;
    }

    this.rutinaAbiertaId = rutina.id;

    if (this.ejerciciosPorRutina[rutina.id]) {
      return;
    }

    this.cargarEjercicios(rutina.id);
  }

  cargarEjercicios(rutinaId: string): void {
    this.cargandoEjerciciosId = rutinaId;

    this.rutinaService.getEjerciciosPorRutina(rutinaId).subscribe({
      next: (ejercicios) => {
        this.ejerciciosPorRutina[rutinaId] =
          (ejercicios || []).sort((a, b) => (a.orden || 0) - (b.orden || 0));

        this.cargandoEjerciciosId = null;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando ejercicios de rutina', err);
        this.error = this.obtenerMensajeError(err, 'No se pudieron cargar los ejercicios de la rutina.');
        this.cargandoEjerciciosId = null;
        this.cdr.detectChanges();
      }
    });
  }

  iniciarSesionHoy(): void {
    this.error = '';
    this.mensajeExito = '';

    if (!this.planActivo) {
      this.error = 'No tienes un plan activo para iniciar una sesión.';
      this.cdr.detectChanges();
      return;
    }

    if (!this.rutinaAbiertaId) {
      this.error = 'Primero abre una rutina para iniciar la sesión de hoy.';
      this.cdr.detectChanges();
      return;
    }

    const rutina = this.rutinas.find(r => r.id === this.rutinaAbiertaId) || null;

    if (!rutina) {
      this.error = 'No se pudo identificar la rutina seleccionada.';
      this.cdr.detectChanges();
      return;
    }

    const ejercicios = this.obtenerEjercicios(rutina.id);

    if (ejercicios.length === 0) {
      this.error = 'Esta rutina no tiene ejercicios asignados para registrar.';
      this.cdr.detectChanges();
      return;
    }

    this.rutinaSesion = rutina;
    this.ejerciciosSesion = ejercicios;
    this.datosSesion = {};

    ejercicios.forEach(ejercicio => {
      this.datosSesion[ejercicio.ejercicioId] = {
        ejercicioId: ejercicio.ejercicioId,
        ejercicioNombre: ejercicio.ejercicioNombre,
        seriesRealizadas: ejercicio.series || 0,
        repeticionesRealizadas: ejercicio.repeticiones || 0,
        pesoUsado: ejercicio.pesoSugerido || 0,
        completado: true
      };
    });

    this.observacionCliente = '';
    this.modoSesionActiva = true;
    this.cdr.detectChanges();
  }

  cambiarCompletado(ejercicioId: string, completado: boolean): void {
    if (!this.datosSesion[ejercicioId]) {
      return;
    }

    this.datosSesion[ejercicioId].completado = completado;
  }

  finalizarEntrenamiento(): void {
    this.error = '';
    this.mensajeExito = '';

    if (!this.planActivo || !this.rutinaSesion) {
      this.error = 'No se pudo identificar el plan o la rutina de la sesión.';
      this.cdr.detectChanges();
      return;
    }

    const ejerciciosRealizados = Object.values(this.datosSesion);

    if (ejerciciosRealizados.length === 0) {
      this.error = 'Debes tener al menos un ejercicio para finalizar el entrenamiento.';
      this.cdr.detectChanges();
      return;
    }

    const request: EntrenamientoRealizadoCrearRequest = {
      planId: this.planActivo.id,
      rutinaId: this.rutinaSesion.id,
      rutinaNombre: this.rutinaSesion.nombre,
      ejerciciosRealizados,
      observacionCliente: this.observacionCliente
    };

    this.guardandoEntrenamiento = true;
    this.cdr.detectChanges();

    this.entrenamientoRealizadoService.registrarEntrenamiento(request).subscribe({
      next: () => {
        this.guardandoEntrenamiento = false;
        this.mensajeExito = 'Entrenamiento registrado correctamente.';
        this.cancelarSesion();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error registrando entrenamiento', err);
        this.error = this.obtenerMensajeError(err, 'No se pudo registrar el entrenamiento.');
        this.guardandoEntrenamiento = false;
        this.cdr.detectChanges();
      }
    });
  }

  cancelarSesion(): void {
    this.modoSesionActiva = false;
    this.rutinaSesion = null;
    this.ejerciciosSesion = [];
    this.datosSesion = {};
    this.observacionCliente = '';
    this.guardandoEntrenamiento = false;
  }

  obtenerEjercicios(rutinaId: string): RutinaEjercicioDTO[] {
    return this.ejerciciosPorRutina[rutinaId] || [];
  }

  volver(): void {
    this.router.navigate(['/cliente']);
  }

  actualizar(): void {
    this.cargarMisRutinas();
  }

  private normalizarActivo(valor: any): boolean {
    return valor === true || valor === 'true' || valor === 1;
  }

  private normalizarTexto(valor: string | null | undefined): string {
    return (valor || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toUpperCase()
      .trim();
  }

  private obtenerMensajeError(err: any, mensajePorDefecto: string): string {
    if (err?.status === 403) {
      return 'No tienes permisos para realizar esta acción.';
    }

    return (
      err?.error?.mensaje ||
      err?.error?.message ||
      err?.error?.error ||
      err?.message ||
      mensajePorDefecto
    );
  }
}