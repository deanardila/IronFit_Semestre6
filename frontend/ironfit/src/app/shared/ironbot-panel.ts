import { Component, EventEmitter, Input, Output } from '@angular/core';
import { IronBotResponse } from './ironbot';

type IronBotVista = 'todas' | 'estado' | 'alerta' | 'recomendacion';

@Component({
  selector: 'app-ironbot-panel',
  standalone: false,
  template: `
    <section class="ironbot-panel" *ngIf="cargando || respuesta || error">
      <div class="ironbot-loading" *ngIf="cargando">Analizando información...</div>
      <div class="ironbot-error" *ngIf="!cargando && error">{{ error }}</div>

      <ng-container *ngIf="!cargando && respuesta">
        <div class="ironbot-unavailable" *ngIf="!respuesta.disponible">
          {{ respuesta.mensaje }}
        </div>

        <ng-container *ngIf="respuesta.disponible">
          <div class="ironbot-actions-row">
            <div class="ironbot-tabs" role="group" aria-label="Secciones del análisis">
              <button type="button" (click)="vista='todas'" [class.active]="vista==='todas'">Resumen</button>
              <button *ngIf="partes.alerta" type="button" (click)="vista='alerta'" [class.active]="vista==='alerta'">Ver alerta</button>
              <button *ngIf="partes.recomendacion" type="button" (click)="vista='recomendacion'" [class.active]="vista==='recomendacion'">Recomendación</button>
            </div>
            <button *ngIf="mostrarBotonActualizar" type="button" class="ironbot-refresh-btn" [disabled]="cargando" (click)="reanalizar.emit()">Actualizar análisis</button>
          </div>

          <div class="ironbot-grid">
            <article class="ironbot-card" *ngIf="partes.estado && (vista==='todas' || vista==='estado')">
              <span>Estado del seguimiento</span>
              <p>{{ partes.estado }}</p>
            </article>
            <article class="ironbot-card ironbot-card--alert" *ngIf="partes.alerta && (vista==='todas' || vista==='alerta')">
              <span>Aspecto por revisar</span>
              <p>{{ partes.alerta }}</p>
            </article>
            <article class="ironbot-card ironbot-card--recommendation" *ngIf="partes.recomendacion && (vista==='todas' || vista==='recomendacion')">
              <span>Acción recomendada</span>
              <p>{{ partes.recomendacion }}</p>
            </article>
          </div>
        </ng-container>
      </ng-container>
      <div class="ironbot-footnotes" *ngIf="respuesta">
        <span *ngIf="respuesta.fuente">Fuente: {{ respuesta.fuente }}</span>
        <span *ngIf="respuesta.disponible">Análisis informativo basado en los datos disponibles en IronFit.</span>
      </div>
    </section>
  `,
  styles: [`

    :host { display:block; font-family:'DM Sans',system-ui,sans-serif; }
    .ironbot-panel { box-sizing:border-box; container-type:inline-size; margin:0; padding:22px; border:1px solid #303035; border-radius:14px; background:#171719; color:#f7f7f8; }
    .ironbot-loading,.ironbot-error,.ironbot-unavailable { padding:15px; border:1px solid #39393e; border-radius:10px; background:#202023; font:500 13px/1.55 'DM Sans',sans-serif; text-align:left; }
    .ironbot-error { color:#ff9aa0; border-color:rgba(241,29,37,.35); }
    .ironbot-actions-row { display:flex; align-items:center; justify-content:space-between; gap:12px; min-width:0; margin:0 0 18px; }
    .ironbot-tabs { display:flex; align-items:center; flex-wrap:wrap; gap:9px; min-width:0; }
    .ironbot-tabs button,.ironbot-refresh-btn { box-sizing:border-box; display:inline-flex; justify-content:center; align-items:center; min-height:44px; padding:10px 14px; border:1px solid #39393e; border-radius:9px; background:#202023; color:#eee; font:700 12px/1.35 'DM Sans',sans-serif; text-align:center; white-space:nowrap; cursor:pointer; }
    .ironbot-tabs button.active { background:rgba(241,29,37,.12); border-color:#f11d25; color:#fff; }
    .ironbot-tabs button:hover:not(:disabled) { background:#29292d; border-color:#f11d25; color:#fff; }
    .ironbot-refresh-btn { margin-left:auto; flex-shrink:0; border-color:#f11d25; background:#f11d25; color:#fff; }
    .ironbot-refresh-btn:hover:not(:disabled) { background:#d71a21; }
    .ironbot-refresh-btn:disabled { opacity:.5; cursor:not-allowed; }
    .ironbot-tabs button:focus-visible,.ironbot-refresh-btn:focus-visible { outline:2px solid #FFBD60; outline-offset:3px; }
    .ironbot-grid { display:grid; grid-template-columns:repeat(auto-fit,minmax(195px,1fr)); gap:12px; }
    .ironbot-card { box-sizing:border-box; min-width:0; padding:17px; background:#202023; border:1px solid #39393e; border-radius:11px; }
    .ironbot-card span { display:block; margin-bottom:7px; color:#a1a1aa; font:800 11px/1.4 'DM Sans',sans-serif; }
    .ironbot-card p { margin:0; color:#e4e4e7; white-space:pre-line; overflow-wrap:anywhere; font:500 13px/1.65 'DM Sans',sans-serif; }
    .ironbot-card--alert { border-color:rgba(241,29,37,.35); }
    .ironbot-card--recommendation { border-color:rgba(255,189,96,.32); }
    .ironbot-footnotes { display:flex; justify-content:space-between; flex-wrap:wrap; gap:9px; margin-top:15px; color:#a1a1aa; font:500 11px/1.5 'DM Sans',sans-serif; }
    @container (max-width: 730px) {
      .ironbot-actions-row { flex-direction:column; align-items:stretch; }
      .ironbot-tabs { width:100%; }
      .ironbot-tabs button { flex:1 1 120px; white-space:normal; }
      .ironbot-refresh-btn { width:100%; margin-left:0; }
    }
    @container (max-width: 440px) {
      .ironbot-grid { grid-template-columns:1fr; }
    }
    @media (max-width:760px) { .ironbot-panel { padding:16px; } }
  `]
})
export class IronBotPanel {
  @Input() respuesta: IronBotResponse | null = null;
  @Input() cargando = false;
  @Input() error = '';
  @Input() mostrarBotonActualizar = true;
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
