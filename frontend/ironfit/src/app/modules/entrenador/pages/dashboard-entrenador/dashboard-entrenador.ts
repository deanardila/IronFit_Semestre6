import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { Chart, ChartConfiguration, ChartOptions, registerables } from 'chart.js';
import { finalize, timeout } from 'rxjs';

import {
  Dashboard,
  DashboardEntrenadorDTO,
  GraficoDatoDTO
} from '../../../admin/dashboard';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard-entrenador',
  standalone: false,
  templateUrl: './dashboard-entrenador.html',
  styleUrl: './dashboard-entrenador.scss',
})
export class DashboardEntrenador implements OnInit {

  nombreUsuario = '';
  showMenu = false;

  cargando = false;
  error = '';

  fechaDashboard = '';

  data: DashboardEntrenadorDTO | null = null;

  clientesActivos = 0;
  planesActivos = 0;
  sesionesMes = 0;
  cumplimientoMes = 0;
  evaluacionesPendientes = 0;
  rutinasActivas = 0;

  asistenciaData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets: [{ data: [], label: 'Asistencia %', tension: 0.35, fill: true, pointRadius: 4 }]
  };

  progresoClienteData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [{ data: [], label: 'Cumplimiento %' }]
  };

  planesEstadoData: ChartConfiguration<'doughnut'>['data'] = {
    labels: [],
    datasets: [{ data: [] }]
  };

  evaluacionesClienteData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [{ data: [], label: 'Evaluaciones' }]
  };

  lineOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { labels: { color: '#f5f5f5' } }
    },
    scales: {
      x: { ticks: { color: '#9ca3af' }, grid: { color: 'rgba(255,255,255,0.08)' } },
      y: { ticks: { color: '#9ca3af' }, grid: { color: 'rgba(255,255,255,0.08)' }, min: 0, max: 100 }
    }
  };

  barOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'y',
    plugins: {
      legend: { labels: { color: '#f5f5f5' } }
    },
    scales: {
      x: { ticks: { color: '#9ca3af' }, grid: { color: 'rgba(255,255,255,0.08)' }, min: 0 },
      y: { ticks: { color: '#9ca3af' }, grid: { color: 'rgba(255,255,255,0.08)' } }
    }
  };

  doughnutOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: { color: '#f5f5f5' }
      }
    }
  };

  constructor(
    private router: Router,
    private dashboard: Dashboard,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.nombreUsuario = localStorage.getItem('nombreUsuario') ?? 'Entrenador';
    this.fechaDashboard = this.construirFechaDashboard();
    this.cargarDashboard();
  }

  private construirFechaDashboard(): string {
    const fecha = new Date();
    return fecha.toLocaleDateString('es-CO', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  cargarDashboard(): void {
    this.cargando = true;
    this.error = '';
    this.cdr.detectChanges();

    this.dashboard.getDashboardEntrenador()
      .pipe(
        timeout(15000),
        finalize(() => {
          this.cargando = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (res) => {

          if (!res) {
            this.error = 'No se recibieron métricas del dashboard.';
            return;
          }

          this.data = res;

          this.clientesActivos = res.clientesActivos ?? 0;
          this.planesActivos = res.planesActivos ?? 0;
          this.sesionesMes = res.sesionesMes ?? 0;
          this.cumplimientoMes = res.cumplimientoMes ?? 0;
          this.evaluacionesPendientes = res.evaluacionesPendientes ?? 0;
          this.rutinasActivas = res.rutinasActivas ?? 0;

          this.construirGraficas(res);
        },
        error: (err) => {
          console.error('Error cargando dashboard entrenador', err);
          this.error = 'No se pudieron cargar las métricas del entrenador.';
        }
      });
  }

  construirGraficas(res: DashboardEntrenadorDTO): void {
    this.asistenciaData = this.construirLineChart(res.asistenciaUltimosDias || [], 'Asistencia %');
    this.progresoClienteData = this.construirBarChart(res.progresoPorCliente || [], 'Cumplimiento %');
    this.planesEstadoData = this.construirDoughnutChart(res.planesPorEstado || []);
    this.evaluacionesClienteData = this.construirBarChart(res.evaluacionesPorCliente || [], 'Evaluaciones');
  }

  construirLineChart(datos: GraficoDatoDTO[], label: string): ChartConfiguration<'line'>['data'] {
    const datosValidos = datos && datos.length > 0
      ? datos
      : [
          { label: 'Sin datos', valor: 0 }
        ];

    return {
      labels: datosValidos.map(d => d.label),
      datasets: [{ data: datosValidos.map(d => d.valor), label, tension: 0.35, fill: true, pointRadius: 4 }]
    };
  }

  construirBarChart(datos: GraficoDatoDTO[], label: string): ChartConfiguration<'bar'>['data'] {
    const datosValidos = datos && datos.length > 0
      ? datos
      : [
          { label: 'Sin datos', valor: 0 }
        ];

    return {
      labels: datosValidos.map(d => d.label),
      datasets: [{ data: datosValidos.map(d => d.valor), label }]
    };
  }

  construirDoughnutChart(datos: GraficoDatoDTO[]): ChartConfiguration<'doughnut'>['data'] {
    const datosValidos = this.tieneDatos(datos)
      ? datos
      : [
          { label: 'Sin registros', valor: 1 }
        ];

    return {
      labels: datosValidos.map(d => d.label),
      datasets: [{ data: datosValidos.map(d => d.valor) }]
    };
  }

  private tieneDatos(datos: GraficoDatoDTO[] | undefined | null): boolean {
    return !!datos && datos.length > 0 && datos.some(d => (d.valor ?? 0) > 0);
  }

  toggleMenu(): void {
    this.showMenu = !this.showMenu;
  }

  closeMenu(): void {
    this.showMenu = false;
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('rol');
    localStorage.removeItem('nombreUsuario');
    this.router.navigate(['/login']);
  }
}