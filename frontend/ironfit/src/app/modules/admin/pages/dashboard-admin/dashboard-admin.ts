import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { Chart, ChartConfiguration, ChartOptions, registerables } from 'chart.js';
import { finalize, timeout } from 'rxjs';
import { Dashboard, DashboardAdminDTO, GraficoDatoDTO } from '../../dashboard';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard-admin',
  standalone: false,
  templateUrl: './dashboard-admin.html',
  styleUrl: './dashboard-admin.scss',
})
export class DashboardAdmin implements OnInit {

  nombreUsuario = '';
  showMenu = false;

  cargando = false;
  error = '';
  fechaDashboard = '';
  data: DashboardAdminDTO | null = null;

  usuariosActivos = 0;
  clientesActivos = 0;
  entrenadoresActivos = 0;
  planesActivos = 0;
  asistenciasMes = 0;
  asistenciaGlobal = 0;
  evaluacionesRegistradas = 0;
  clientesSinPlan = 0;

  lineChartData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Asistencia %',
        tension: 0.35,
        fill: true,
        pointRadius: 4,
      }
    ]
  };

  barEntrenadoresData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Planes',
      }
    ]
  };

  doughnutRolesData: ChartConfiguration<'doughnut'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
      }
    ]
  };

  barObjetivosData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Planes por objetivo',
      }
    ]
  };

  chartOptions: ChartOptions = {
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
      x: {
        ticks: { color: '#9ca3af' },
        grid: { color: 'rgba(255,255,255,0.08)' }
      },
      y: {
        ticks: { color: '#9ca3af' },
        grid: { color: 'rgba(255,255,255,0.08)' }
      }
    }
  };

  doughnutOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
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
    this.nombreUsuario = localStorage.getItem('nombreUsuario') ?? 'Admin';
    this.fechaDashboard = this.construirFechaDashboard();
    this.cargarDashboard();
  }

  private construirFechaDashboard(): string {
    const fecha = new Date();

    return fecha.toLocaleDateString('es-CO', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }

  cargarDashboard(): void {
    this.cargando = true;
    this.error = '';
    this.cdr.detectChanges();

    this.dashboard.getDashboardAdmin()
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

          this.usuariosActivos = res.usuariosActivos ?? 0;
          this.clientesActivos = res.clientesActivos ?? 0;
          this.entrenadoresActivos = res.entrenadoresActivos ?? 0;
          this.planesActivos = res.planesActivos ?? 0;
          this.asistenciasMes = res.asistenciasMes ?? 0;
          this.asistenciaGlobal = res.asistenciaGlobal ?? 0;
          this.evaluacionesRegistradas = res.evaluacionesRegistradas ?? 0;
          this.clientesSinPlan = res.clientesSinPlan ?? 0;

          this.construirGraficas(res);
        },
        error: (err) => {
          console.error('Error cargando dashboard admin', err);
          this.error = 'No se pudieron cargar las métricas del dashboard.';
        }
      });
  }
  construirGraficas(res: DashboardAdminDTO): void {
    this.lineChartData = this.construirLineChart(res.asistenciaUltimosDias || [], 'Asistencia %');
    this.barEntrenadoresData = this.construirBarChart(res.planesPorEntrenador || [], 'Planes');
    this.doughnutRolesData = this.construirDoughnutChart(res.usuariosPorRol || []);
    this.barObjetivosData = this.construirBarChart(res.planesPorObjetivo || [], 'Objetivos');
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