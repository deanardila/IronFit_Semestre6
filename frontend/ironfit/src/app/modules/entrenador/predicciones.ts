import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PrediccionDetalleDTO {
    clienteId: string;
    planId: string;
    tipo: 'ADHERENCIA' | 'RENDIMIENTO';
    prediccion: string;
    modelo: string;
    probabilidades: Record<string, number>;
    }

    export interface PrediccionPlanDTO {
    clienteId: string;
    planId: string;
    adherencia: PrediccionDetalleDTO;
    rendimiento: PrediccionDetalleDTO;
    }

    @Injectable({
    providedIn: 'root'
    })
    export class PrediccionesService {

    private baseUrl = `${environment.apiUrl}/api/predicciones`;

    constructor(private http: HttpClient) {}

    predecirPlan(planId: string): Observable<PrediccionPlanDTO> {
        return this.http.get<PrediccionPlanDTO>(
        `${this.baseUrl}/plan/${planId}`
        );
    }
}