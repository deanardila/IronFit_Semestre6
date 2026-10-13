import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { BaseChartDirective } from 'ng2-charts';
import { DashboardEntrenador } from './pages/dashboard-entrenador/dashboard-entrenador';
import { MisClientes } from './pages/mis-clientes/mis-clientes';
import { MisPlanes } from './pages/mis-planes/mis-planes';
import { MisRutinas } from './pages/mis-rutinas/mis-rutinas';
import { Asistencias } from './pages/asistencias/asistencias';
import { Evaluaciones } from './pages/evaluaciones/evaluaciones';
import { MiPerfil } from '../cliente/pages/mi-perfil/mi-perfil';
import { SharedModule } from '../../shared/shared-module';
import { RutinaEjercicios } from './pages/rutina-ejercicios/rutina-ejercicios';

const routes: Routes = [
  { path: '', component: DashboardEntrenador },
  { path: 'perfil', component: MiPerfil },
  { path: 'MisClientes', component: MisClientes },
  { path: 'MisPlanes', component: MisPlanes },
  { path: 'MisRutinas', component: MisRutinas },
  { path: 'MisRutinas/:planId', component: MisRutinas },
  { path: 'RutinaEjercicios/:rutinaId', component: RutinaEjercicios },
  { path: 'asistencias', component: Asistencias },
  { path: 'evaluaciones', component: Evaluaciones },
  
];

@NgModule({
  declarations: [
    DashboardEntrenador,
    MisClientes,
    MisPlanes,
    MisRutinas,
    Asistencias,
    Evaluaciones,
    RutinaEjercicios
  ],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    BaseChartDirective,
    RouterModule.forChild(routes)
  ]
})
export class EntrenadorModule { }