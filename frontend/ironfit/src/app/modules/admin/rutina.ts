import { environment } from '../../../environments/environment';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RutinaDTO {
  id: string;
  planId: string;
  nombre: string;
  descripcion: string;
  diaSemana: string;
  orden: number;
  activo: boolean;
  fechaCreacion?: string;
  fechaActualizacion?: string;
}

export interface RutinaRequest {
  planId?: string;
  nombre: string;
  descripcion: string;
  diaSemana: string;
  orden: number;
  activo?: boolean;
}

export interface RutinaEjercicioDTO {
  id: string;
  rutinaId: string;
  ejercicioId: string;

  ejercicioNombre: string;
  ejercicioDescripcion: string;
  categoria: string;
  grupoMuscular: string;
  tipoEquipo: string;

  series: number;
  repeticiones: number;
  descansoSegundos: number;
  tiempoSegundos: number;
  pesoSugerido: number;
  orden: number;
}

export interface RutinaEjercicioRequest {
  rutinaId?: string;
  ejercicioId?: string;
  series: number;
  repeticiones: number;
  descansoSegundos: number;
  tiempoSegundos: number;
  pesoSugerido: number;
  orden: number;
}

@Injectable({
  providedIn: 'root',
})
export class RutinaService {
  private apiRutinas = `${environment.apiUrl}/api/rutinas`;
  private apiRutinaEjercicios = `${environment.apiUrl}/api/rutina-ejercicios`;

  constructor(private http: HttpClient) {}

  // CLIENTE / ADMIN / ENTRENADOR:
  // Lista solo rutinas activas del plan.
  getRutinasPorPlan(planId: string): Observable<RutinaDTO[]> {
    return this.http.get<RutinaDTO[]>(`${this.apiRutinas}/plan/${planId}`);
  }

  // ENTRENADOR / ADMIN:
  // Lista rutinas activas e inactivas para gestión.
  getRutinasPorPlanGestion(planId: string): Observable<RutinaDTO[]> {
    return this.http.get<RutinaDTO[]>(`${this.apiRutinas}/plan/${planId}/gestion`);
  }

  // ENTRENADOR:
  // Crea una rutina dentro de un plan.
  crearRutina(request: RutinaRequest): Observable<RutinaDTO> {
    return this.http.post<RutinaDTO>(this.apiRutinas, request);
  }

  // ENTRENADOR:
  // Actualiza una rutina.
  actualizarRutina(id: string, request: RutinaRequest): Observable<RutinaDTO> {
    return this.http.put<RutinaDTO>(`${this.apiRutinas}/${id}`, request);
  }

  // ENTRENADOR:
  // Activa o inactiva una rutina.
  cambiarEstadoRutina(id: string, activo: boolean): Observable<RutinaDTO> {
    return this.http.patch<RutinaDTO>(
      `${this.apiRutinas}/${id}/estado?activo=${activo}`,
      {}
    );
  }

  // Rutina ejercicios:
  // Lista ejercicios de una rutina.
  getEjerciciosPorRutina(rutinaId: string): Observable<RutinaEjercicioDTO[]> {
    return this.http.get<RutinaEjercicioDTO[]>(
      `${this.apiRutinaEjercicios}/rutina/${rutinaId}`
    );
  }

  // Crea un ejercicio dentro de una rutina.
  crearEjercicioRutina(request: RutinaEjercicioRequest): Observable<RutinaEjercicioDTO> {
    return this.http.post<RutinaEjercicioDTO>(
      `${this.apiRutinaEjercicios}`,
      request
    );
  }

  
  // Actualiza un ejercicio dentro de una rutina.
  actualizarEjercicioRutina(
    id: string,
    request: RutinaEjercicioRequest
  ): Observable<RutinaEjercicioDTO> {
    return this.http.put<RutinaEjercicioDTO>(
      `${this.apiRutinaEjercicios}/${id}`,
      request
    );
  }

  // Elimina un ejercicio de una rutina.
  eliminarEjercicioRutina(id: string): Observable<void> {
    return this.http.delete<void>(
      `${this.apiRutinaEjercicios}/${id}`
    );
  }
}