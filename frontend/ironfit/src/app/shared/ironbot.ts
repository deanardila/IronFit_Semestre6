import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface IronBotResponse {
  disponible: boolean;
  tipo: string;
  titulo: string;
  mensaje: string;
  modelo: string;
  fuente: string;
}

@Injectable({ providedIn: 'root' })
export class IronBotService {
  private readonly baseUrl = `${environment.apiUrl}/api/ironbot`;

  constructor(private http: HttpClient) {}

  resumenAdmin(): Observable<IronBotResponse> {
    return this.http.get<IronBotResponse>(`${this.baseUrl}/admin/resumen`);
  }

  resumenCliente(): Observable<IronBotResponse> {
    return this.http.get<IronBotResponse>(`${this.baseUrl}/cliente/resumen`);
  }

  analizarPlan(planId: string): Observable<IronBotResponse> {
    return this.http.get<IronBotResponse>(`${this.baseUrl}/entrenador/plan/${planId}`);
  }
}
