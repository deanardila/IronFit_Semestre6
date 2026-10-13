import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BaseChartDirective } from 'ng2-charts';
import { DashboardCliente } from './pages/dashboard-cliente/dashboard-cliente';
import { MiPlan } from './pages/mi-plan/mi-plan';
import { MisRutinas } from './pages/mis-rutinas/mis-rutinas';
import { MisAsistencias } from './pages/mis-asistencias/mis-asistencias';
import { MisEvaluaciones } from './pages/mis-evaluaciones/mis-evaluaciones';
import { MiPerfil } from './pages/mi-perfil/mi-perfil';
import { SharedModule } from '../../shared/shared-module';

const routes: Routes = [
  { path: '', component: DashboardCliente },
  { path: 'perfil', component: MiPerfil },
  { path: 'plan', component: MiPlan },
  { path: 'MisRutinas', component: MisRutinas },
  { path: 'asistencias', component: MisAsistencias },
  { path: 'evaluaciones', component: MisEvaluaciones },
];

@NgModule({
  declarations: [
    DashboardCliente,
    MiPlan,
    MisRutinas,
    MisAsistencias,
    MisEvaluaciones
  ],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    BaseChartDirective,
    RouterModule.forChild(routes),
  ]
})
export class ClienteModule { }