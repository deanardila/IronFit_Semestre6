import { ChangeDetectorRef,Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PlanEntrenamientoDTO, Planes } from '../../../admin/planes';

@Component({
  selector: 'app-mi-plan',
  standalone: false,
  templateUrl: './mi-plan.html',
  styleUrl: './mi-plan.scss',
})
export class MiPlan implements OnInit {

  cargando = false;
  error = '';

  planes: PlanEntrenamientoDTO[] = [];
  planActivo: PlanEntrenamientoDTO | null = null;

  constructor(
    private planesService: Planes,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarMiPlan();
  }

  cargarMiPlan(): void {
    this.cargando = true;
    this.error = '';

    this.planesService.getPlanes().subscribe({
      next: (planes) => {
        this.planes = planes || [];

        this.planActivo =
          this.planes.find(plan => this.normalizarActivo(plan.activo)) || null;

        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando mi plan', err);

        this.error = this.obtenerMensajeError(
          err,
          'No se pudo cargar tu plan de entrenamiento.'
        );

        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  volver(): void {
    this.router.navigate(['/cliente']);
  }

  actualizar(): void {
    this.cargarMiPlan();
  }

  irAMisRutinas(): void {
    this.router.navigate(['/cliente/mis-rutinas']);
  }

  private normalizarActivo(valor: any): boolean {
    return valor === true || valor === 'true' || valor === 1;
  }

  private obtenerMensajeError(err: any, mensajePorDefecto: string): string {
    if (err?.status === 403) {
      return 'No tienes permisos para consultar este plan. Verifica que hayas iniciado sesión como cliente.';
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