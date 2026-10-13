import { environment } from '../../../environments/environment';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PlanEntrenamientoDTO {
    clienteNombre?: string;
    entrenadorNombre?: string;
    id: string;
    nombre: string;
    descripcion: string;
    objetivo: string;
    fechaInicio: string;
    fechaFin: string;
    clienteId: string;
    entrenadorId: string;
    activo: boolean;
    fechaCreacion: string;
    fechaActualizacion: string;
}

export interface PlanEntrenamientoCrearDTO {
    nombre: string;
    descripcion: string;
    objetivo: string;
    fechaInicio: string;
    fechaFin: string;
    clienteId: string;
    entrenadorId: string;
}

export interface PaginaResponse<T> {
    contenido: T[];
    pagina: number;
    tamano: number;
    totalElementos: number;
    totalPaginas: number;
    ultima: boolean;
}

export interface PlanResumen {
    idPlan: number;
    nombrePlan: string;
    nombreCliente: string;
    nombreEntrenador: string;
    estado: string | { name?: string };
    fechaInicio?: string;
    fechaFin?: string;
}

export interface RutinaResumen {
    idRutina: number;
    nombre: string;
    descripcion?: string;
}

@Injectable({
    providedIn: 'root',
})
export class Planes {
    private apiUrl = `${environment.apiUrl}/api/planes`;

    constructor(private http: HttpClient) {}

    getPlanes(): Observable<PlanEntrenamientoDTO[]> {
        return this.http.get<PlanEntrenamientoDTO[]>(this.apiUrl);
    }

    getPlanesPaginados(
        page: number,
        size: number,
        buscar?: string,
        activo?: boolean | null
    ): Observable<PaginaResponse<PlanEntrenamientoDTO>> {
        const params: any = {
            page,
            size
        };

        if (buscar && buscar.trim()) {
            params.buscar = buscar.trim();
        }

        if (activo !== null && activo !== undefined) {
            params.activo = activo;
        }

        return this.http.get<PaginaResponse<PlanEntrenamientoDTO>>(
            `${this.apiUrl}/paginado`,
            { params }
        );
    }

    getResumenPlanes(): Observable<PlanResumen[]> {
        return this.http.get<PlanResumen[]>(`${this.apiUrl}/resumen`);
    }

    getRutinasPorPlan(idPlan: number): Observable<RutinaResumen[]> {
        return this.http.get<RutinaResumen[]>(`${this.apiUrl}/${idPlan}/rutinas`);
    }

    crearPlan(plan: PlanEntrenamientoCrearDTO): Observable<PlanEntrenamientoDTO> {
        return this.http.post<PlanEntrenamientoDTO>(this.apiUrl, plan);
    }

    actualizarPlan(id: string, plan: PlanEntrenamientoCrearDTO): Observable<PlanEntrenamientoDTO> {
        return this.http.put<PlanEntrenamientoDTO>(`${this.apiUrl}/${id}`, plan);
    }

    cambiarEstado(id: string, activo: boolean): Observable<PlanEntrenamientoDTO> {
        return this.http.patch<PlanEntrenamientoDTO>(`${this.apiUrl}/${id}/estado?activo=${activo}`, {});
    }

    obtenerPlan(id: string): Observable<PlanEntrenamientoDTO> {
        return this.http.get<PlanEntrenamientoDTO>(`${this.apiUrl}/${id}`);
    }
}