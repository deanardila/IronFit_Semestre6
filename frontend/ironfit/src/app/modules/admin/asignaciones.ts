import { environment } from '../../../environments/environment';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AsignacionEntrenadorClienteDTO {
    id: string;

    clienteId: string;
    clienteNombre: string;
    clienteCorreo: string;
    clienteDocumento: string;

    entrenadorId: string;
    entrenadorNombre: string;
    entrenadorCorreo: string;

    activo: boolean;
    fechaAsignacion: string;
    fechaActualizacion: string;
}

export interface AsignacionEntrenadorClienteRequest {
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

@Injectable({
    providedIn: 'root',
})
export class AsignacionesService {
    private apiUrl = `${environment.apiUrl}/api/asignaciones`;

    constructor(private http: HttpClient) {}

    listarAsignaciones(): Observable<AsignacionEntrenadorClienteDTO[]> {
        return this.http.get<AsignacionEntrenadorClienteDTO[]>(this.apiUrl);
    }

    listarMisClientes(): Observable<AsignacionEntrenadorClienteDTO[]> {
        return this.http.get<AsignacionEntrenadorClienteDTO[]>(`${this.apiUrl}/mis-clientes`);
    }

    listarMisClientesPaginado(
        page: number,
        size: number,
        buscar?: string
    ): Observable<PaginaResponse<AsignacionEntrenadorClienteDTO>> {
        const params: any = {
            page,
            size
        };

        if (buscar && buscar.trim()) {
            params.buscar = buscar.trim();
        }

        return this.http.get<PaginaResponse<AsignacionEntrenadorClienteDTO>>(
            `${this.apiUrl}/mis-clientes/paginado`,
            { params }
        );
    }

    crearAsignacion(request: AsignacionEntrenadorClienteRequest): Observable<AsignacionEntrenadorClienteDTO> {
        return this.http.post<AsignacionEntrenadorClienteDTO>(this.apiUrl, request);
    }

    cambiarEstado(id: string, activo: boolean): Observable<AsignacionEntrenadorClienteDTO> {
        return this.http.patch<AsignacionEntrenadorClienteDTO>(
            `${this.apiUrl}/${id}/estado?activo=${activo}`,
            {}
        );
    }
}