import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface PlanNutricionalDTO {
  id: string;
  planId: string;
  planNombre: string;
  clienteId: string;
  clienteNombre: string;
  entrenadorId: string;
  entrenadorNombre: string;
  kcalObjetivo: number;
  proteinaG: number;
  carbohidratosG: number;
  grasaG: number;
  descripcion: string;
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface PlanNutricionalRequestDTO {
  planId: string;
  kcalObjetivo: number | null;
  proteinaG: number | null;
  carbohidratosG: number | null;
  grasaG: number | null;
  descripcion: string;
}

@Injectable({ providedIn: 'root' })
export class NutricionService {
  private readonly apiUrl = `${environment.apiUrl}/api/nutricion`;

  constructor(private http: HttpClient) {}

  listar(): Observable<PlanNutricionalDTO[]> {
    return this.http.get<PlanNutricionalDTO[]>(this.apiUrl);
  }

  obtenerPorPlan(planId: string): Observable<PlanNutricionalDTO> {
    return this.http.get<PlanNutricionalDTO>(`${this.apiUrl}/plan/${planId}`);
  }

  crear(request: PlanNutricionalRequestDTO): Observable<PlanNutricionalDTO> {
    return this.http.post<PlanNutricionalDTO>(this.apiUrl, request);
  }

  actualizar(id: string, request: PlanNutricionalRequestDTO): Observable<PlanNutricionalDTO> {
    return this.http.put<PlanNutricionalDTO>(`${this.apiUrl}/${id}`, request);
  }
}
