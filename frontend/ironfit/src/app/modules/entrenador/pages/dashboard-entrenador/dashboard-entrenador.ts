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

// Paleta visual IronFit; no modifica valores ni consultas.
const CHART_RED = '#f11d25';
const CHART_RED_SOFT = 'rgba(241, 29, 37, 0.14)';
const CHART_AMBER = '#ffbd60';
const CHART_AMBER_HOVER = '#ffd08b';
const CHART_TEXT = '#a1a1aa';
const CHART_GRID = 'rgba(255, 255, 255, 0.07)';
const CHART_GRAY = '#62626b';
const CHART_TOOLTIP_BG = '#202023';


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
    animation: { duration: 350 },
    plugins: {
      legend: { labels: { color: CHART_TEXT, font: { family: 'DM Sans', size: 12 }, usePointStyle: true, boxWidth: 9 } },
      tooltip: {
        backgroundColor: CHART_TOOLTIP_BG,
        titleColor: '#ffffff',
        bodyColor: '#e4e4e7',
        borderColor: '#404046',
        borderWidth: 1,
        padding: 12,
        displayColors: true
      }
    },
    scales: {
      x: {
        ticks: { color: CHART_TEXT, font: { family: 'DM Sans' } },
        grid: { color: CHART_GRID },
        border: { display: false }
      },
      y: {
        ticks: { color: CHART_TEXT, font: { family: 'DM Sans' } },
        grid: { color: CHART_GRID },
        border: { display: false },
        min: 0,
        max: 100
      }
    }
  };

  barOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'y',
    animation: { duration: 350 },
    plugins: {
      legend: { labels: { color: CHART_TEXT, font: { family: 'DM Sans', size: 12 }, usePointStyle: true, boxWidth: 9 } },
      tooltip: {
        backgroundColor: CHART_TOOLTIP_BG,
        titleColor: '#ffffff',
        bodyColor: '#e4e4e7',
        borderColor: '#404046',
        borderWidth: 1,
        padding: 12
      }
    },
    scales: {
      x: {
        ticks: { color: CHART_TEXT, font: { family: 'DM Sans' } },
        grid: { color: CHART_GRID },
        border: { display: false },
        min: 0
      },
      y: {
        ticks: { color: CHART_TEXT, font: { family: 'DM Sans' } },
        grid: { display: false },
        border: { display: false }
      }
    }
  };

  doughnutOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '68%',
    animation: { duration: 350 },
    plugins: {
      legend: {
        position: 'bottom',
        labels: { color: CHART_TEXT, font: { family: 'DM Sans', size: 12 }, usePointStyle: true, boxWidth: 9, padding: 16 }
      },
      tooltip: {
        backgroundColor: CHART_TOOLTIP_BG,
        titleColor: '#ffffff',
        bodyColor: '#e4e4e7',
        borderColor: '#404046',
        borderWidth: 1,
        padding: 12
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
        timeout(45000),
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
      datasets: [{ data: datosValidos.map(d => d.valor), label, tension: 0.35, fill: true, pointRadius: 3, pointHoverRadius: 5, pointBackgroundColor: CHART_RED, pointBorderColor: '#171719', pointBorderWidth: 2, borderColor: CHART_RED, backgroundColor: CHART_RED_SOFT, borderWidth: 2.5 }]
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
      datasets: [{ data: datosValidos.map(d => d.valor), label, backgroundColor: label === 'Evaluaciones' ? CHART_AMBER : CHART_RED, hoverBackgroundColor: label === 'Evaluaciones' ? CHART_AMBER_HOVER : '#ff5158', borderRadius: 5, borderSkipped: false, maxBarThickness: 24 }]
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
      datasets: [{ data: datosValidos.map(d => d.valor), backgroundColor: datosValidos.map((d, i) => { const estado = d.label.toLowerCase(); return estado.includes('inactiv') ? CHART_GRAY : estado.includes('activ') ? CHART_RED : [CHART_GRAY, '#9898a1', '#404047'][i % 3]; }), hoverOffset: 5, borderWidth: 0, spacing: 3 }]
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