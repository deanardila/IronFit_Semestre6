import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { Chart, ChartConfiguration, ChartOptions, registerables } from 'chart.js';
import { finalize, timeout } from 'rxjs';
import { Dashboard, DashboardAdminDTO, GraficoDatoDTO } from '../../dashboard';
import { IronBotResponse, IronBotService } from '../../../../shared/ironbot';

Chart.register(...registerables);

// Paleta de presentación del dashboard ADMIN, sin alterar los datos.
const ADMIN_RED = '#f11d25';
const ADMIN_AMBER = '#ffbd60';
const ADMIN_TEXT = '#a1a1aa';
const ADMIN_GRID = 'rgba(255,255,255,0.07)';
const ADMIN_TOOLTIP = '#202023';


@Component({
  selector: 'app-dashboard-admin',
  standalone: false,
  templateUrl: './dashboard-admin.html',
  styleUrl: './dashboard-admin.scss',
})
export class DashboardAdmin implements OnInit {

  nombreUsuario = '';
  showMenu = false;

  // El perfil se refresca después del login: el getter muestra el nombre
  // cuando llega al caché, sin exponer el correo en el encabezado.
  get nombreBienvenida(): string {
    try {
      const perfil = JSON.parse(localStorage.getItem('miPerfilCache') || 'null');
      const nombres = typeof perfil?.nombres === 'string' ? perfil.nombres.trim() : '';
      if (nombres && !nombres.includes('@')) return nombres;
    } catch {
      // Si el caché no es válido, usamos el nombre de sesión.
    }
    const guardado = this.nombreUsuario.trim();
    return guardado && !guardado.includes('@') ? guardado : 'Administrador';
  }

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

  ironBotRespuesta: IronBotResponse | null = null;
  cargandoIronBot = false;
  errorIronBot = '';

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
    animation: { duration: 350 },
    plugins: {
      legend: {
        labels: {
          color: ADMIN_TEXT,
          font: { family: 'DM Sans', size: 12 },
          usePointStyle: true,
          boxWidth: 9
        }
      },
      tooltip: {
        backgroundColor: ADMIN_TOOLTIP,
        titleColor: '#ffffff',
        bodyColor: '#e4e4e7',
        borderColor: '#404046',
        borderWidth: 1,
        padding: 12
      }
    },
    scales: {
      x: {
        ticks: { color: ADMIN_TEXT, font: { family: 'DM Sans' } },
        grid: { color: ADMIN_GRID },
        border: { display: false }
      },
      y: {
        ticks: { color: ADMIN_TEXT, font: { family: 'DM Sans' } },
        grid: { color: ADMIN_GRID },
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
        labels: {
          color: ADMIN_TEXT,
          font: { family: 'DM Sans', size: 12 },
          usePointStyle: true,
          boxWidth: 9,
          padding: 16
        }
      },
      tooltip: {
        backgroundColor: ADMIN_TOOLTIP,
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
    private cdr: ChangeDetectorRef,
    private ironBot: IronBotService
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

  analizarConIronBot(): void {
    if (this.cargandoIronBot) {
      return;
    }

    this.cargandoIronBot = true;
    this.errorIronBot = '';

    this.ironBot.resumenAdmin()
      .pipe(
        timeout(45000),
        finalize(() => {
          this.cargandoIronBot = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (res) => {
          this.ironBotRespuesta = res;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando IronBot admin', err);
          this.errorIronBot = 'No se pudo completar el análisis de IronBot.';
          this.cdr.detectChanges();
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
      datasets: [{ data: datosValidos.map(d => d.valor), label, tension: 0.35, fill: true, pointRadius: 3, pointHoverRadius: 5, pointBackgroundColor: ADMIN_RED, pointBorderColor: '#171719', pointBorderWidth: 2, borderColor: ADMIN_RED, backgroundColor: 'rgba(241,29,37,0.14)', borderWidth: 2.5 }]
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
      datasets: [{ data: datosValidos.map(d => d.valor), label, backgroundColor: datosValidos.map((_, i) => i % 2 === 0 ? ADMIN_RED : ADMIN_AMBER), hoverBackgroundColor: datosValidos.map((_, i) => i % 2 === 0 ? '#ff5158' : '#ffd08b'), borderRadius: 5, borderSkipped: false, maxBarThickness: 25 }]
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
      datasets: [{ data: datosValidos.map(d => d.valor), backgroundColor: datosValidos.map((_, i) => [ADMIN_RED, ADMIN_AMBER, '#62626b', '#9898a1'][i % 4]), borderWidth: 0, spacing: 3, hoverOffset: 5 }]
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