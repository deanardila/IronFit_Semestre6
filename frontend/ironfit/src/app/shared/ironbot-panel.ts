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
          <span class="ironbot-kicker">IRONBOT · IRONFIT</span>
          <h3>{{ respuesta?.titulo || 'Asistente de análisis' }}</h3>
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
            <button *ngIf="partes.alerta" type="button" (click)="vista='alerta'" [class.active]="vista==='alerta'">Ver alerta</button>
            <button *ngIf="partes.recomendacion" type="button" (click)="vista='recomendacion'" [class.active]="vista==='recomendacion'">Recomendación</button>
            <button type="button" class="retry" [disabled]="cargando" (click)="reanalizar.emit()">Volver a analizar</button>
          </div>

          <div class="ironbot-grid">
            <article class="ironbot-card" *ngIf="partes.estado && (vista==='todas' || vista==='estado')">
              <span>Estado</span>
              <p>{{ partes.estado }}</p>
            </article>
            <article class="ironbot-card ironbot-card--alert" *ngIf="partes.alerta && (vista==='todas' || vista==='alerta')">
              <span>Alerta</span>
              <p>{{ partes.alerta }}</p>
            </article>
            <article class="ironbot-card ironbot-card--recommendation" *ngIf="partes.recomendacion && (vista==='todas' || vista==='recomendacion')">
              <span>Recomendación</span>
              <p>{{ partes.recomendacion }}</p>
            </article>
          </div>
        </ng-container>
      </ng-container>
      <p class="ironbot-disclaimer" *ngIf="respuesta?.disponible">Análisis informativo basado en los datos disponibles en IronFit.</p>
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

    :host{display:block;font-family:'DM Sans',system-ui,sans-serif}
    .ironbot-panel{margin:0;padding:22px;border:1px solid #303035;border-radius:14px;background:#171719;color:#f7f7f8}
    .ironbot-header{align-items:center;padding-bottom:17px;border-bottom:1px solid #303035}
    .ironbot-kicker{color:#f11d25;font:800 11px 'DM Sans',sans-serif;letter-spacing:.10em}
    .ironbot-header h3{font:800 19px/1.3 'DM Sans',sans-serif;letter-spacing:0;color:#fff;margin:7px 0 0}
    .ironbot-source{color:#a1a1aa;font:500 11px 'DM Sans',sans-serif}
    .ironbot-actions{margin:18px 0 14px;gap:8px}
    .ironbot-actions button{border:1px solid #39393e;border-radius:9px;padding:10px 13px;background:#202023;color:#eee;font:700 12px 'DM Sans',sans-serif}
    .ironbot-actions button.active{background:rgba(241,29,37,.12);border-color:#f11d25;color:#fff}
    .ironbot-actions button:hover:not(:disabled){background:#29292d;border-color:#f11d25;color:#fff}
    .ironbot-actions .retry{border-color:#f11d25;background:#f11d25;color:#fff}
    .ironbot-actions .retry:hover:not(:disabled){background:#d71a21}
    .ironbot-grid{grid-template-columns:repeat(auto-fit,minmax(195px,1fr));gap:12px}
    .ironbot-card{min-width:0;background:#202023;border:1px solid #39393e;border-radius:11px;padding:17px}
    .ironbot-card span{font:800 11px 'DM Sans',sans-serif;letter-spacing:.04em;text-transform:none;color:#a1a1aa}
    .ironbot-card p{color:#e4e4e7;white-space:pre-line;overflow-wrap:anywhere;font:500 13px/1.65 'DM Sans',sans-serif}
    .ironbot-card--alert{border-color:rgba(241,29,37,.35)}
    .ironbot-card--recommendation{border-color:rgba(255,189,96,.32)}
    .ironbot-loading,.ironbot-error,.ironbot-unavailable{margin-top:16px;background:#202023;border:1px solid #39393e;border-radius:10px;padding:15px;font:500 13px/1.55 'DM Sans',sans-serif}
    .ironbot-error{color:#ff9aa0;border-color:rgba(241,29,37,.35)}
    .ironbot-disclaimer{margin:16px 0 0;color:#a1a1aa;font:500 11px/1.45 'DM Sans',sans-serif}
    .ironbot-actions button:disabled{opacity:.5;cursor:not-allowed}
    @media(max-width:760px){.ironbot-panel{padding:16px}.ironbot-grid{grid-template-columns:1fr}}
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
    const lineas = mensaje.split(/\r?\n/).map(linea => linea.trim()).filter(Boolean);
    const extraer = (prefijos: string[]) => {
      const linea = lineas.find(valor => prefijos.some(prefijo =>
        valor.toLocaleUpperCase('es').startsWith(prefijo)));
      return linea ? linea.substring(linea.indexOf(':') + 1).trim() : '';
    };
    const estado = extraer(['ESTADO:']);
    const alerta = extraer(['ALERTA:']);
    const recomendacion = extraer(['RECOMENDACION:', 'RECOMENDACIÓN:']);
    // Mostrar solamente contenido que el backend haya enviado.
    if (estado || alerta || recomendacion) return { estado, alerta, recomendacion };
    return { estado: mensaje, alerta: '', recomendacion: '' };
  }
}
