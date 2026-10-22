import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Login } from './auth/login/login';
import { AuthGuard } from './auth/auth-guard';
import { RoleGuard } from './auth/role-guard';
import { PlanesRutinas } from './modules/admin/pages/planes-rutinas/planes-rutinas';
import { AuditoriaReportes } from './modules/admin/pages/auditoria-reportes/auditoria-reportes';

const routes: Routes = [
    {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },

    {
    path: 'admin',
    loadChildren: () =>
      import('./modules/admin/admin-module').then(m => m.AdminModule),
  },  

    {
    path: 'entrenador',
    loadChildren: () =>
      import('./modules/entrenador/entrenador-module').then(m => m.EntrenadorModule),
  },

    {
    path: 'cliente',
    loadChildren: () =>
      import('./modules/cliente/cliente-module').then(m => m.ClienteModule),
  },

  {
    path: 'login',
    component: Login
  },

  {
  path: 'admin',
  canActivate: [AuthGuard, RoleGuard],
  data: { role: 'ADMIN' },
  loadChildren: () =>
    import('./modules/admin/admin-module').then(m => m.AdminModule),
},

{
  path: 'entrenador',
  canActivate: [AuthGuard, RoleGuard],
  data: { role: 'ENTRENADOR' },
  loadChildren: () =>
    import('./modules/entrenador/entrenador-module').then(m => m.EntrenadorModule),
},

{
  path: 'cliente',
  canActivate: [AuthGuard, RoleGuard],
  data: { role: 'CLIENTE' },
  loadChildren: () =>
    import('./modules/cliente/cliente-module').then(m => m.ClienteModule),
},

{ path: 'admin/planes', component: PlanesRutinas },

{ path: 'planes/:id/rutina', component: PlanesRutinas },

{
  path: 'auditoria-reportes',
  component: AuditoriaReportes
}

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
