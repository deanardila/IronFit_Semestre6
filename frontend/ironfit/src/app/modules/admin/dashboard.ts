import { environment } from '../../../environments/environment';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface GraficoDatoDTO {
    label: string;
    valor: number;
    }

    export interface DashboardAdminDTO {
    usuariosActivos: number;
    clientesActivos: number;
    entrenadoresActivos: number;
    planesActivos: number;
    asistenciasMes: number;
    asistenciaGlobal: number;
    evaluacionesRegistradas: number;
    clientesSinPlan: number;

    usuariosPorRol: GraficoDatoDTO[];
    planesPorObjetivo: GraficoDatoDTO[];
    planesPorEntrenador: GraficoDatoDTO[];
    asistenciaUltimosDias: GraficoDatoDTO[];
    }

    export interface DashboardEntrenadorDTO {
    clientesActivos: number;
    planesActivos: number;
    sesionesMes: number;
    cumplimientoMes: number;
    evaluacionesPendientes: number;
    rutinasActivas: number;

    asistenciaUltimosDias: GraficoDatoDTO[];
    progresoPorCliente: GraficoDatoDTO[];
    planesPorEstado: GraficoDatoDTO[];
    evaluacionesPorCliente: GraficoDatoDTO[];
    }

    export interface DashboardClienteDTO {
    planActual: string;
    rutinasActivas: number;
    sesionesMes: number;
    rachaActual: number;
    progresoPlan: number;
    pesoActual: number;
    imcActual: number;

    asistenciaUltimosDias: GraficoDatoDTO[];
    evolucionPeso: GraficoDatoDTO[];
    metricasCorporales: GraficoDatoDTO[];
    progresoPlanGrafico: GraficoDatoDTO[];
    }

    @Injectable({
    providedIn: 'root'
    })
    export class Dashboard {

    private baseUrl = `${environment.apiUrl}/api/dashboard`;

    constructor(private http: HttpClient) {}

    getDashboardAdmin(): Observable<DashboardAdminDTO> {
        return this.http.get<DashboardAdminDTO>(`${this.baseUrl}/admin`);
    }

    getDashboardEntrenador(): Observable<DashboardEntrenadorDTO> {
        return this.http.get<DashboardEntrenadorDTO>(`${this.baseUrl}/entrenador`);
    }

    getDashboardCliente(): Observable<DashboardClienteDTO> {
        return this.http.get<DashboardClienteDTO>(`${this.baseUrl}/cliente`);
    }
}