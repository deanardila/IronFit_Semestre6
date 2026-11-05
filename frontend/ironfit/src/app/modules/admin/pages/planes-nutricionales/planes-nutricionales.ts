import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { timeout } from 'rxjs';
import { NutricionService, PlanNutricionalDTO } from '../../../../shared/nutricion';

@Component({
  selector: 'app-planes-nutricionales-admin',
  standalone: false,
  templateUrl: './planes-nutricionales.html',
  styleUrls: ['./planes-nutricionales.scss'],
})
export class PlanesNutricionalesAdmin implements OnInit {
  private readonly requestTimeoutMs = 45000;

  planes: PlanNutricionalDTO[] = [];
  planesFiltrados: PlanNutricionalDTO[] = [];
  terminoBusqueda = '';
  cargando = true;
  error = '';

  constructor(
    private nutricionService: NutricionService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.error = '';

    this.nutricionService.listar()
      .pipe(timeout(this.requestTimeoutMs))
      .subscribe({
        next: (planes) => {
          this.planes = planes || [];
          this.aplicarFiltroLocal();
          this.cargando = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.error = err?.error?.mensaje || err?.error?.message || 'No se pudieron cargar los planes nutricionales.';
          this.planes = [];
          this.planesFiltrados = [];
          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  buscar(): void {
    this.aplicarFiltroLocal();
    this.cdr.detectChanges();
  }

  private aplicarFiltroLocal(): void {
    const q = this.terminoBusqueda.trim().toLowerCase();

    if (!q) {
      this.planesFiltrados = [...this.planes];
      return;
    }

    this.planesFiltrados = this.planes.filter(p =>
      [
        p.planNombre,
        p.clienteNombre,
        p.entrenadorNombre,
        String(p.kcalObjetivo ?? ''),
        String(p.proteinaG ?? ''),
        String(p.carbohidratosG ?? ''),
        String(p.grasaG ?? '')
      ].some(valor => (valor || '').toString().toLowerCase().includes(q))
    );
  }

  volver(): void {
    this.router.navigate(['/admin']);
  }
}
