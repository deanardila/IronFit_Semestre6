import { environment } from '../../../environments/environment';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EjercicioDTO {
    id: string;
    nombre: string;
    descripcion: string;
    categoria: string;
    grupoMuscular: string;
    seriesSugeridas: number | null;
    repeticionesSugeridas: number | null;
    tipoEquipo: string | null;
}

export interface EjercicioCrearDTO {
    nombre: string;
    descripcion: string;
    categoria: string;
    grupoMuscular: string;
    seriesSugeridas: number | null;
    repeticionesSugeridas: number | null;
    tipoEquipo: string | null;
}

export interface PaginaResponse<T> {
    contenido: T[];
    pagina: number;
    tamano: number;
    totalElementos: number;
    totalPaginas: number;
    ultima: boolean;
}

@Injectable({
    providedIn: 'root',
})
export class Ejercicios {
    private apiUrl = `${environment.apiUrl}/api/ejercicios`;

    constructor(private http: HttpClient) {}

    getEjercicios(): Observable<EjercicioDTO[]> {
        return this.http.get<EjercicioDTO[]>(this.apiUrl);
    }

    getEjerciciosPaginados(
        page: number,
        size: number,
        buscar?: string,
        categoria?: string | null,
        grupoMuscular?: string | null,
        activo?: boolean | null
    ): Observable<PaginaResponse<EjercicioDTO>> {
        const params: any = {
            page,
            size
        };

        if (buscar && buscar.trim()) {
            params.buscar = buscar.trim();
        }

        if (categoria) {
            params.categoria = categoria;
        }

        if (grupoMuscular) {
            params.grupoMuscular = grupoMuscular;
        }

        if (activo !== null && activo !== undefined) {
            params.activo = activo;
        }

        return this.http.get<PaginaResponse<EjercicioDTO>>(
            `${this.apiUrl}/paginado`,
            { params }
        );
    }

    crearEjercicio(ejercicio: EjercicioCrearDTO): Observable<EjercicioDTO> {
        return this.http.post<EjercicioDTO>(this.apiUrl, ejercicio);
    }

    actualizarEjercicio(id: string, ejercicio: EjercicioCrearDTO): Observable<EjercicioDTO> {
        return this.http.put<EjercicioDTO>(`${this.apiUrl}/${id}`, ejercicio);
    }

    eliminarEjercicio(id: string): Observable<void> {
        return this.http.patch<void>(`${this.apiUrl}/${id}/estado`, null, {
            params: { activo: false }
        });
    }
}