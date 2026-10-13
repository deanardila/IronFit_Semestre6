import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { Chart, ChartConfiguration, ChartOptions, registerables } from 'chart.js';
import { finalize, timeout } from 'rxjs';

import {
  Dashboard,
  DashboardClienteDTO,
  GraficoDatoDTO
} from '../../../admin/dashboard';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard-cliente',
  standalone: false,
  templateUrl: './dashboard-cliente.html',
  styleUrl: './dashboard-cliente.scss',
})
export class DashboardCliente implements OnInit {

  nombreUsuario = '';
  showMenu = false;

  cargando = false;
  error = '';
  fechaDashboard = '';
  data: DashboardClienteDTO | null = null;

  planActual = 'Sin plan activo';
  rutinasActivas = 0;
  sesionesMes = 0;
  rachaActual = 0;
  progresoPlan = 0;
  pesoActual = 0;
  imcActual = 0;

  asistenciaData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets: [{ data: [], label: 'Asistencia %', tension: 0.35, fill: true, pointRadius: 4 }]
  };

  pesoData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets: [{ data: [], label: 'Peso corporal', tension: 0.35, fill: true, pointRadius: 4 }]
  };

  progresoPlanData: ChartConfiguration<'doughnut'>['data'] = {
    labels: [],
    datasets: [{ data: [] }]
  };

  metricasData: ChartConfiguration<'radar'>['data'] = {
    labels: [],
    datasets: [{ data: [], label: 'Medidas corporales' }]
  };

  lineOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { labels: { color: '#f5f5f5' } }
    },
    scales: {
      x: { ticks: { color: '#9ca3af' }, grid: { color: 'rgba(255,255,255,0.08)' } },
      y: { ticks: { color: '#9ca3af' }, grid: { color: 'rgba(255,255,255,0.08)' } }
    }
  };

  linePercentOptions: ChartOptions<'line'> = {
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

  radarOptions: ChartOptions<'radar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        labels: {
          color: '#f5f5f5'
        }
      }
    },
    scales: {
      r: {
        min: 0,
        beginAtZero: true,
        ticks: {
          color: '#9ca3af',
          backdropColor: 'transparent'
        },
        grid: {
          color: 'rgba(255,255,255,0.12)'
        },
        angleLines: {
          color: 'rgba(255,255,255,0.12)'
        },
        pointLabels: {
          color: '#f5f5f5'
        }
      }
    }
  };

  constructor(
    private router: Router,
    private dashboard: Dashboard,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.nombreUsuario = localStorage.getItem('nombreUsuario') ?? 'Cliente';
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

    this.dashboard.getDashboardCliente()
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

          this.planActual = res.planActual || 'Sin plan activo';
          this.rutinasActivas = res.rutinasActivas ?? 0;
          this.sesionesMes = res.sesionesMes ?? 0;
          this.rachaActual = res.rachaActual ?? 0;
          this.progresoPlan = res.progresoPlan ?? 0;
          this.pesoActual = res.pesoActual ?? 0;
          this.imcActual = res.imcActual ?? 0;

          this.construirGraficas(res);
        },
        error: (err) => {
          console.error('Error cargando dashboard cliente', err);
          this.error = 'No se pudieron cargar tus métricas.';
        }
      });
  }

  construirGraficas(res: DashboardClienteDTO): void {
    this.asistenciaData = this.construirLineChart(res.asistenciaUltimosDias || [], 'Asistencia %');
    this.pesoData = this.construirLineChart(res.evolucionPeso || [], 'Peso corporal');
    this.progresoPlanData = this.construirDoughnutChart(res.progresoPlanGrafico || []);
    this.metricasData = this.construirRadarChart(res.metricasCorporales || [], 'Medidas corporales');
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

  construirRadarChart(datos: GraficoDatoDTO[], label: string): ChartConfiguration<'radar'>['data'] {
    const datosValidos = this.tieneDatos(datos)
      ? datos
      : [
          { label: 'Pecho', valor: 0 },
          { label: 'Cintura', valor: 0 },
          { label: 'Brazo', valor: 0 },
          { label: 'Pierna', valor: 0 }
        ];

    return {
      labels: datosValidos.map(d => d.label),
      datasets: [
        {
          data: datosValidos.map(d => d.valor),
          label
        }
      ]
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