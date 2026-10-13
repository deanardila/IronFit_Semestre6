import { environment } from '../../../environments/environment';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MedidasCorporalesDTO {
    pecho?: number | null;
    cintura?: number | null;
    brazo?: number | null;
    pierna?: number | null;
}

export interface EvaluacionFisicaDTO {
    id: string;
    clienteId: string;
    clienteNombre?: string;
    entrenadorId: string;
    entrenadorNombre?: string;
    fecha: string;
    pesoCorporal: number;
    talla: number;
    imc: number;
    porcentajeGraso?: number | null;
    medidasCorporales?: MedidasCorporalesDTO | null;
    observaciones?: string | null;
    fechaCreacion?: string;
    fechaActualizacion?: string;
}

export interface EvaluacionFisicaRequestDTO {
    clienteId: string;
    fecha?: string | null;
    pesoCorporal: number | null;
    talla: number | null;
    porcentajeGraso?: number | null;
    pecho?: number | null;
    cintura?: number | null;
    brazo?: number | null;
    pierna?: number | null;
    observaciones?: string | null;
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
    providedIn: 'root'
})
export class EvaluacionesFisicasService {

    private readonly apiUrl = `${environment.apiUrl}/api/evaluaciones-fisicas`;

    constructor(private http: HttpClient) {}

    listarEvaluaciones(): Observable<EvaluacionFisicaDTO[]> {
        return this.http.get<EvaluacionFisicaDTO[]>(this.apiUrl);
    }

    listarEvaluacionesPaginadas(
        page: number,
        size: number,
        buscar?: string
    ): Observable<PaginaResponse<EvaluacionFisicaDTO>> {
        const params: any = {
            page,
            size
        };

        if (buscar && buscar.trim()) {
            params.buscar = buscar.trim();
        }

        return this.http.get<PaginaResponse<EvaluacionFisicaDTO>>(
            `${this.apiUrl}/paginado`,
            { params }
        );
    }

    crearEvaluacion(request: EvaluacionFisicaRequestDTO): Observable<EvaluacionFisicaDTO> {
        return this.http.post<EvaluacionFisicaDTO>(this.apiUrl, request);
    }

    actualizarEvaluacion(id: string, request: EvaluacionFisicaRequestDTO): Observable<EvaluacionFisicaDTO> {
        return this.http.put<EvaluacionFisicaDTO>(`${this.apiUrl}/${id}`, request);
    }

    eliminarEvaluacion(id: string): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }

    listarMisEvaluaciones(): Observable<EvaluacionFisicaDTO[]> {
        return this.http.get<EvaluacionFisicaDTO[]>(`${this.apiUrl}/mis-evaluaciones`);
    }
}