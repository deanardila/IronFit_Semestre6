import { Component, EventEmitter, Input, Output } from '@angular/core';
import { IronBotResponse } from './ironbot';

type IronBotVista = 'todas' | 'estado' | 'alerta' | 'recomendacion';

@Component({
  selector: 'app-ironbot-panel',
  standalone: false,
  template: `
    <section class="ironbot-panel" *ngIf="cargando || respuesta || error">
      <header class="ironbot-header">
        <div>
          <span class="ironbot-kicker">IRON BOT</span>
          <h3>Asistente inteligente de IronFit</h3>
        </div>
        <span class="ironbot-source" *ngIf="respuesta?.fuente">Fuente: {{ respuesta?.fuente }}</span>
      </header>

      <div class="ironbot-loading" *ngIf="cargando">Analizando información...</div>
      <div class="ironbot-error" *ngIf="!cargando && error">{{ error }}</div>

      <ng-container *ngIf="!cargando && respuesta">
        <div class="ironbot-unavailable" *ngIf="!respuesta.disponible">
          {{ respuesta.mensaje }}
        </div>

        <ng-container *ngIf="respuesta.disponible">
          <div class="ironbot-actions">
            <button type="button" (click)="vista='todas'" [class.active]="vista==='todas'">Resumen</button>
            <button type="button" (click)="vista='alerta'" [class.active]="vista==='alerta'">Ver riesgo</button>
            <button type="button" (click)="vista='recomendacion'" [class.active]="vista==='recomendacion'">¿Qué debo revisar?</button>
            <button type="button" class="retry" (click)="reanalizar.emit()">Volver a analizar</button>
          </div>

          <div class="ironbot-grid">
            <article class="ironbot-card" *ngIf="vista==='todas' || vista==='estado'">
              <span>Estado</span>
              <p>{{ partes.estado }}</p>
            </article>
            <article class="ironbot-card ironbot-card--alert" *ngIf="vista==='todas' || vista==='alerta'">
              <span>Alerta</span>
              <p>{{ partes.alerta }}</p>
            </article>
            <article class="ironbot-card ironbot-card--recommendation" *ngIf="vista==='todas' || vista==='recomendacion'">
              <span>Recomendación</span>
              <p>{{ partes.recomendacion }}</p>
            </article>
          </div>
        </ng-container>
      </ng-container>
    </section>
  `,
  styles: [`
    .ironbot-panel{margin:22px 0;padding:20px;border:1px solid rgba(225,29,46,.35);border-radius:16px;background:#121212;color:#f5f5f5}
    .ironbot-header{display:flex;justify-content:space-between;gap:16px;align-items:flex-start;margin-bottom:16px}
    .ironbot-kicker{color:#e11d2e;font-size:.76rem;font-weight:800;letter-spacing:.12em}
    .ironbot-header h3{margin:4px 0 0;font-size:1.08rem}
    .ironbot-source{font-size:.75rem;color:#9ca3af}
    .ironbot-actions{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:14px}
    .ironbot-actions button{border:1px solid #333;background:#1d1d1d;color:#ddd;border-radius:999px;padding:8px 12px;cursor:pointer}
    .ironbot-actions button.active,.ironbot-actions button:hover{border-color:#e11d2e;color:#fff}
    .ironbot-actions .retry{margin-left:auto;background:#e11d2e;border-color:#e11d2e;color:#fff}
    .ironbot-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}
    .ironbot-card{background:#191919;border:1px solid #292929;border-radius:12px;padding:14px}
    .ironbot-card span{display:block;color:#9ca3af;font-size:.76rem;text-transform:uppercase;letter-spacing:.06em;margin-bottom:7px}
    .ironbot-card p{margin:0;line-height:1.45}
    .ironbot-card--alert{border-color:rgba(225,29,46,.45)}
    .ironbot-card--recommendation{border-color:#3f3f46}
    .ironbot-loading,.ironbot-error,.ironbot-unavailable{padding:12px;border-radius:10px;background:#1b1b1b}
    .ironbot-error{color:#fca5a5}
    @media(max-width:760px){.ironbot-grid{grid-template-columns:1fr}.ironbot-header{flex-direction:column}.ironbot-actions .retry{margin-left:0}}
  `]
})
export class IronBotPanel {
  @Input() respuesta: IronBotResponse | null = null;
  @Input() cargando = false;
  @Input() error = '';
  @Output() reanalizar = new EventEmitter<void>();

  vista: IronBotVista = 'todas';

  get partes(): { estado: string; alerta: string; recomendacion: string } {
    const mensaje = this.respuesta?.mensaje?.trim() || '';
    const lineas = mensaje.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
    const valor = (prefijo: string) => {
      const linea = lineas.find(l => l.toUpperCase().startsWith(prefijo));
      return linea ? linea.substring(linea.indexOf(':') + 1).trim() : '';
    };

    const estado = valor('ESTADO:');
    const alerta = valor('ALERTA:');
    const recomendacion = valor('RECOMENDACION:') || valor('RECOMENDACIÓN:');

    if (estado || alerta || recomendacion) {
      return {
        estado: estado || 'Sin novedades relevantes.',
        alerta: alerta || 'No se identificaron alertas destacadas.',
        recomendacion: recomendacion || 'Mantener el seguimiento actual.'
      };
    }

    return {
      estado: mensaje || 'No hay información disponible.',
      alerta: 'No se identificaron alertas adicionales.',
      recomendacion: 'Mantener el seguimiento actual.'
    };
  }
}
