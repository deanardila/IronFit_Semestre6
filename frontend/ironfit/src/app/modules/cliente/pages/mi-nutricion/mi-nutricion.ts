import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { timeout } from 'rxjs';
import { NutricionService, PlanNutricionalDTO } from '../../../../shared/nutricion';

@Component({
  selector: 'app-mi-nutricion',
  standalone: false,
  templateUrl: './mi-nutricion.html',
  styleUrls: ['./mi-nutricion.scss'],
})
export class MiNutricion implements OnInit {
  private readonly requestTimeoutMs = 45000;

  planes: PlanNutricionalDTO[] = [];
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
          this.cargando = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.error = err?.error?.mensaje || err?.error?.message || 'No se pudo cargar tu información nutricional.';
          this.planes = [];
          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  volver(): void {
    this.router.navigate(['/cliente']);
  }
}
