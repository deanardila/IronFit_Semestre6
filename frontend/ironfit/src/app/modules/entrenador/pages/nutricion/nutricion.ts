import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { timeout } from 'rxjs';
import { Planes, PlanEntrenamientoDTO } from '../../../admin/planes';
import {
  NutricionService,
  PlanNutricionalDTO,
  PlanNutricionalRequestDTO
} from '../../../../shared/nutricion';

@Component({
  selector: 'app-nutricion-entrenador',
  standalone: false,
  templateUrl: './nutricion.html',
  styleUrls: ['./nutricion.scss'],
})
export class NutricionEntrenador implements OnInit {
  private readonly requestTimeoutMs = 45000;

  planId = '';
  plan: PlanEntrenamientoDTO | null = null;
  nutricion: PlanNutricionalDTO | null = null;

  cargando = true;
  guardando = false;
  error = '';
  mensaje = '';

  formulario: PlanNutricionalRequestDTO = {
    planId: '',
    kcalObjetivo: null,
    proteinaG: null,
    carbohidratosG: null,
    grasaG: null,
    descripcion: ''
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private planesService: Planes,
    private nutricionService: NutricionService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.planId = this.route.snapshot.paramMap.get('planId') || '';
    this.formulario.planId = this.planId;

    if (!this.planId) {
      this.error = 'No se pudo identificar el plan de entrenamiento.';
      this.cargando = false;
      return;
    }

    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.error = '';
    this.mensaje = '';

    this.planesService.obtenerPlan(this.planId)
      .pipe(timeout(this.requestTimeoutMs))
      .subscribe({
        next: (plan) => {
          this.plan = plan;
          this.cargarNutricion();
        },
        error: (err) => {
          this.error = this.mensajeError(err, 'No se pudo cargar el plan de entrenamiento.');
          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  private cargarNutricion(): void {
    this.nutricionService.obtenerPorPlan(this.planId)
      .pipe(timeout(this.requestTimeoutMs))
      .subscribe({
        next: (nutricion) => {
          this.nutricion = nutricion;
          this.formulario = {
            planId: this.planId,
            kcalObjetivo: nutricion.kcalObjetivo,
            proteinaG: nutricion.proteinaG,
            carbohidratosG: nutricion.carbohidratosG,
            grasaG: nutricion.grasaG,
            descripcion: nutricion.descripcion || ''
          };
          this.cargando = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          if (err?.status === 404 || (err?.error?.message || err?.error?.mensaje || '').toLowerCase().includes('no tiene')) {
            this.nutricion = null;
            this.formulario = {
              planId: this.planId,
              kcalObjetivo: null,
              proteinaG: null,
              carbohidratosG: null,
              grasaG: null,
              descripcion: ''
            };
            this.cargando = false;
            this.cdr.detectChanges();
            return;
          }

          this.error = this.mensajeError(err, 'No se pudo consultar el plan nutricional.');
          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  guardar(): void {
    this.error = '';
    this.mensaje = '';

    if (!this.validarFormulario()) {
      return;
    }

    this.guardando = true;
    const request: PlanNutricionalRequestDTO = {
      ...this.formulario,
      planId: this.planId,
      descripcion: this.formulario.descripcion.trim()
    };

    const operacion = this.nutricion
      ? this.nutricionService.actualizar(this.nutricion.id, request)
      : this.nutricionService.crear(request);

    operacion.pipe(timeout(this.requestTimeoutMs)).subscribe({
      next: (res) => {
        this.nutricion = res;
        this.mensaje = 'Plan nutricional guardado correctamente.';
        this.guardando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = this.mensajeError(err, 'No se pudo guardar el plan nutricional.');
        this.guardando = false;
        this.cdr.detectChanges();
      }
    });
  }

  private validarFormulario(): boolean {
    const campos = [
      { valor: this.formulario.kcalObjetivo, nombre: 'Las calorías objetivo' },
      { valor: this.formulario.proteinaG, nombre: 'La proteína' },
      { valor: this.formulario.carbohidratosG, nombre: 'Los carbohidratos' },
      { valor: this.formulario.grasaG, nombre: 'La grasa' }
    ];

    const invalido = campos.find(c => c.valor === null || c.valor === undefined || Number(c.valor) <= 0);

    if (invalido) {
      this.error = `${invalido.nombre} debe ser mayor que cero.`;
      return false;
    }

    if (!this.formulario.descripcion?.trim()) {
      this.error = 'La descripción del plan nutricional es obligatoria.';
      return false;
    }

    return true;
  }

  volver(): void {
    this.router.navigate(['/entrenador/MisPlanes']);
  }

  private mensajeError(err: any, fallback: string): string {
    return err?.error?.mensaje || err?.error?.message || err?.error?.error || err?.message || fallback;
  }
}
