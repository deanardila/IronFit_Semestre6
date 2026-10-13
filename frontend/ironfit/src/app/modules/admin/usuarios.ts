import { environment } from '../../../environments/environment';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UsuarioResumen {
    id: string;
    tipoDoc: string;
    nroDoc: string;
    numDoc: string;
    nombres: string;
    apellidos: string;
    telefono?: string;
    correo?: string;
    activo?: boolean;
    roles: string[]; // ["ROLE_CLIENTE", "ROLE_ENTRENADOR"]
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
export class Usuarios {

    private baseUrl = `${environment.apiUrl}/api/usuarios`;

    constructor(private http: HttpClient) {}

    getUsuariosResumen(): Observable<UsuarioResumen[]> {
        return this.http.get<UsuarioResumen[]>(`${this.baseUrl}/resumen`);
    }

    getUsuariosPaginados(
        page: number,
        size: number,
        buscar?: string,
        rol?: string | null,
        activo?: boolean | null
    ): Observable<PaginaResponse<UsuarioResumen>> {
        const params: any = {
            page,
            size
        };

        if (buscar && buscar.trim()) {
            params.buscar = buscar.trim();
        }

        if (rol) {
            params.rol = rol;
        }

        if (activo !== null && activo !== undefined) {
            params.activo = activo;
        }

        return this.http.get<PaginaResponse<UsuarioResumen>>(
            `${this.baseUrl}/paginado`,
            { params }
        );
    }

    crearUsuario(usuario: any): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/crear`, usuario);
    }
    cambiarEstado(id: string, activo: boolean): Observable<UsuarioResumen> {
    return this.http.patch<UsuarioResumen>(`${this.baseUrl}/${id}/estado`, { activo });
    }

    getMiPerfil(): Observable<UsuarioResumen> {
        return this.http.get<UsuarioResumen>(`${this.baseUrl}/me`);
    }

    actualizarMiPerfil(data: any) {
    return this.http.patch<UsuarioResumen>(`${this.baseUrl}/me`, data);
    }

    cambiarMiPassword(nuevaPassword: string) {
    return this.http.patch(`${this.baseUrl}/me/password`, nuevaPassword, {
        headers: { 'Content-Type': 'text/plain' }
    });
    }

    resetearPassword(id: string) {
    return this.http.patch<UsuarioResumen>(`${this.baseUrl}/${id}/resetear-password`, {});
    }
}
