import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { Chart, ChartConfiguration, ChartOptions, registerables } from 'chart.js';
import { finalize, timeout } from 'rxjs';

import {
  Dashboard,
  DashboardClienteDTO,
  GraficoDatoDTO
} from '../../../admin/dashboard';
import { IronBotResponse, IronBotService } from '../../../../shared/ironbot';

Chart.register(...registerables);

// Paleta visual del dashboard; no modifica los datos de entrenamiento.
const CLIENT_RED = '#f11d25';
const CLIENT_AMBER = '#ffbd60';
const CLIENT_TEXT = '#a1a1aa';
const CLIENT_GRID = 'rgba(255,255,255,0.07)';
const CLIENT_TOOLTIP = '#202023';


@Component({
  selector: 'app-dashboard-cliente',
  standalone: false,
  templateUrl: './dashboard-cliente.html',
  styleUrl: './dashboard-cliente.scss',
})
export class DashboardCliente implements OnInit {

  nombreUsuario = '';
  showMenu = false;

  // Preferimos el nombre del perfil y evitamos mostrar un correo como saludo.
  get nombreBienvenida(): string {
    try {
      const perfil = JSON.parse(localStorage.getItem('miPerfilCache') || 'null');
      const nombres = typeof perfil?.nombres === 'string' ? perfil.nombres.trim() : '';
      if (nombres && !nombres.includes('@')) return nombres;
    } catch {
      // Si el caché no es válido, conservamos el nombre de sesión.
    }
    const guardado = this.nombreUsuario.trim();
    return guardado && !guardado.includes('@') ? guardado : 'Cliente';
  }


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

  ironBotRespuesta: IronBotResponse | null = null;
  cargandoIronBot = false;
  errorIronBot = '';

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
    animation: { duration: 350 },
    plugins: { legend: { labels: { color: CLIENT_TEXT, font: { family: 'DM Sans', size: 12 }, usePointStyle: true, boxWidth: 9 } }, tooltip: { backgroundColor: CLIENT_TOOLTIP, titleColor: '#fff', bodyColor: '#e4e4e7', borderColor: '#404046', borderWidth: 1, padding: 12 } },
    scales: {
      x: { ticks: { color: CLIENT_TEXT, font: { family: 'DM Sans' } }, grid: { color: CLIENT_GRID }, border: { display: false } },
      y: { ticks: { color: CLIENT_TEXT, font: { family: 'DM Sans' } }, grid: { color: CLIENT_GRID }, border: { display: false } }
    }
  };

  linePercentOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    animation: { duration: 350 },
    plugins: { legend: { labels: { color: CLIENT_TEXT, font: { family: 'DM Sans', size: 12 }, usePointStyle: true, boxWidth: 9 } }, tooltip: { backgroundColor: CLIENT_TOOLTIP, titleColor: '#fff', bodyColor: '#e4e4e7', borderColor: '#404046', borderWidth: 1, padding: 12 } },
    scales: {
      x: { ticks: { color: CLIENT_TEXT, font: { family: 'DM Sans' } }, grid: { color: CLIENT_GRID }, border: { display: false } },
      y: { ticks: { color: CLIENT_TEXT, font: { family: 'DM Sans' } }, grid: { color: CLIENT_GRID }, border: { display: false }, min: 0, max: 100 }
    }
  };

  doughnutOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '68%',
    animation: { duration: 350 },
    plugins: {
      legend: { position: 'bottom', labels: { ...{ color: CLIENT_TEXT, font: { family: 'DM Sans', size: 12 }, usePointStyle: true, boxWidth: 9 }, padding: 16 } },
      tooltip: { backgroundColor: CLIENT_TOOLTIP, titleColor: '#fff', bodyColor: '#e4e4e7', borderColor: '#404046', borderWidth: 1, padding: 12 }
    }
  };

  radarOptions: ChartOptions<'radar'> = {
    responsive: true,
    maintainAspectRatio: false,
    animation: { duration: 350 },
    plugins: { legend: { labels: { color: CLIENT_TEXT, font: { family: 'DM Sans', size: 12 }, usePointStyle: true, boxWidth: 9 } }, tooltip: { backgroundColor: CLIENT_TOOLTIP, titleColor: '#fff', bodyColor: '#e4e4e7', borderColor: '#404046', borderWidth: 1, padding: 12 } },
    scales: {
      r: {
        min: 0,
        beginAtZero: true,
        ticks: { color: CLIENT_TEXT, backdropColor: 'transparent', font: { family: 'DM Sans' } },
        grid: { color: CLIENT_GRID },
        angleLines: { color: CLIENT_GRID },
        pointLabels: { color: '#e4e4e7', font: { family: 'DM Sans' } }
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


  analizarConIronBot(): void {
    if (this.cargandoIronBot) {
      return;
    }

    this.cargandoIronBot = true;
    this.errorIronBot = '';

    this.ironBot.resumenCliente()
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
          console.error('Error cargando IronBot cliente', err);
          this.errorIronBot = 'No se pudo explicar tu progreso en este momento.';
          this.cdr.detectChanges();
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