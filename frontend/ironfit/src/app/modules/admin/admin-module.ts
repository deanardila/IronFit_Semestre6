import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';

import { BaseChartDirective } from 'ng2-charts';
import { DashboardAdmin } from './pages/dashboard-admin/dashboard-admin';
import { GestionUsuarios } from './pages/gestion-usuarios/gestion-usuarios';
import { GestionEjercicios } from './pages/gestion-ejercicios/gestion-ejercicios';
import { PlanesRutinas } from './pages/planes-rutinas/planes-rutinas';
import { AuditoriaReportes } from './pages/auditoria-reportes/auditoria-reportes';
import { FormsModule } from '@angular/forms';
import { MiPerfil } from '../cliente/pages/mi-perfil/mi-perfil';
import { SharedModule } from '../../shared/shared-module';
import { Asignaciones } from './pages/asignaciones/asignaciones';



const routes: Routes = [
  { path: '', component: DashboardAdmin },
  { path: 'perfil', component: MiPerfil },
  { path: 'usuarios', component: GestionUsuarios },
  { path: 'asignaciones', component: Asignaciones },
  { path: 'ejercicios', component: GestionEjercicios },
  { path: 'planes-rutinas', component: PlanesRutinas },
  { path: 'auditoria', component: AuditoriaReportes },
];


@NgModule({
  declarations: [
    DashboardAdmin,
    GestionUsuarios,
    GestionEjercicios,
    PlanesRutinas,
    AuditoriaReportes,
    Asignaciones
  ],
  imports: [
  CommonModule,
  FormsModule,
  SharedModule,
  BaseChartDirective,
  RouterModule.forChild(routes),
]
})
export class AdminModule { }
