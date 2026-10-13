import { environment } from '../../../environments/environment';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EjercicioRealizadoRequest {
    ejercicioId: string;
    ejercicioNombre: string;
    seriesRealizadas: number;
    repeticionesRealizadas: number;
    pesoUsado: number;
    completado: boolean;
    }

    export interface EntrenamientoRealizadoCrearRequest {
    planId: string;
    rutinaId: string;
    rutinaNombre: string;
    ejerciciosRealizados: EjercicioRealizadoRequest[];
    observacionCliente?: string;
    }

    export interface EjercicioRealizadoResponse {
    ejercicioId: string;
    ejercicioNombre: string;
    seriesRealizadas: number;
    repeticionesRealizadas: number;
    pesoUsado: number;
    completado: boolean;
    }

    export interface EntrenamientoRealizadoResponse {
    id: string;
    clienteId: string;
    clienteNombre: string;
    planId: string;
    rutinaId: string;
    rutinaNombre: string;
    fecha: string;
    estado: string;
    ejerciciosRealizados: EjercicioRealizadoResponse[];
    observacionCliente: string;
    fechaCreacion: string;
    fechaActualizacion: string;
    }

    @Injectable({
    providedIn: 'root',
    })
    export class EntrenamientoRealizadoService {
    private apiUrl = `${environment.apiUrl}/api/entrenamientos-realizados`;

    constructor(private http: HttpClient) {}

    registrarEntrenamiento(
        request: EntrenamientoRealizadoCrearRequest
    ): Observable<EntrenamientoRealizadoResponse> {
        return this.http.post<EntrenamientoRealizadoResponse>(this.apiUrl, request);
    }

    listarMisEntrenamientos(): Observable<EntrenamientoRealizadoResponse[]> {
        return this.http.get<EntrenamientoRealizadoResponse[]>(`${this.apiUrl}/mis-entrenamientos`);
    }
}