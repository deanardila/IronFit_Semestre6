import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UsuarioResumen, Usuarios } from '../../../admin/usuarios';

@Component({
  selector: 'app-mi-perfil',
  standalone: false,
  templateUrl: './mi-perfil.html',
  styleUrls: ['./mi-perfil.scss'],
})
export class MiPerfil implements OnInit {

  private readonly perfilCacheKey = 'miPerfilCache';

  perfil: UsuarioResumen = {
    id: '',
    nombres: '',
    apellidos: '',
    correo: '',
    telefono: '',
    tipoDoc: '',
    nroDoc: '',
    numDoc: '',
    roles: [],
    activo: true
  };

  password = {
    nueva: '',
    confirmar: ''
  };

  cargando = false;
  mensajeExito = '';
  mensajeError = '';

  constructor(
    private usuariosApi: Usuarios,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.hidratarPerfilDesdeCache();
    this.cargarPerfil();
  }

  cargarPerfil(): void {
    this.cargando = true;
    this.mensajeExito = '';
    this.mensajeError = '';

    this.usuariosApi.getMiPerfil().subscribe({
      next: (data: UsuarioResumen) => {
        this.perfil = data;
        this.guardarPerfilEnCache(data);
        this.cargando = false;
      },
      error: (err: unknown) => {
        console.error('Error cargando perfil', err);
        this.mensajeError = 'No se pudo cargar la información del perfil';
        this.cargando = false;
      }
    });
  }

  guardarPerfil(): void {
    this.mensajeExito = '';
    this.mensajeError = '';

    const payload: Partial<UsuarioResumen> = {
      nombres: this.perfil.nombres,
      apellidos: this.perfil.apellidos,
      correo: this.perfil.correo,
      telefono: this.perfil.telefono
    };

    this.usuariosApi.actualizarMiPerfil(payload).subscribe({
      next: (data: UsuarioResumen) => {
        this.perfil = data;
        this.guardarPerfilEnCache(data);
        this.mensajeExito = 'Perfil actualizado correctamente';
      },
      error: (err: unknown) => {
        console.error('Error actualizando perfil', err);
        this.mensajeError = 'No se pudo actualizar el perfil';
      }
    });
  }

  cambiarPassword(): void {
    this.mensajeExito = '';
    this.mensajeError = '';

    if (!this.password.nueva || !this.password.confirmar) {
      this.mensajeError = 'Debes completar ambos campos de contraseña';
      return;
    }

    if (this.password.nueva !== this.password.confirmar) {
      this.mensajeError = 'Las contraseñas no coinciden';
      return;
    }

    this.usuariosApi.cambiarMiPassword(this.password.nueva).subscribe({
      next: () => {
        this.password = { nueva: '', confirmar: '' };
        this.mensajeExito = 'Contraseña actualizada correctamente';
      },
      error: (err: unknown) => {
        console.error('Error cambiando contraseña', err);
        this.mensajeError = 'No se pudo actualizar la contraseña';
      }
    });
  }

  obtenerRolPrincipal(): string {
    return this.perfil.roles && this.perfil.roles.length > 0
      ? this.perfil.roles[0]
      : '-';
  }

  volverAlDashboard(): void {
    const rol = localStorage.getItem('rol');

    switch (rol) {
      case 'ADMIN':
        this.router.navigate(['/admin']);
        break;
      case 'ENTRENADOR':
        this.router.navigate(['/entrenador']);
        break;
      default:
        this.router.navigate(['/cliente']);
        break;
    }
  }

  private hidratarPerfilDesdeCache(): void {
    const perfilCacheRaw = localStorage.getItem(this.perfilCacheKey);
    if (!perfilCacheRaw) {
      return;
    }

    try {
      const perfilCache = JSON.parse(perfilCacheRaw) as UsuarioResumen;
      this.perfil = { ...this.perfil, ...perfilCache };
    } catch {
      localStorage.removeItem(this.perfilCacheKey);
    }
  }

  private guardarPerfilEnCache(perfil: UsuarioResumen): void {
    localStorage.setItem(this.perfilCacheKey, JSON.stringify(perfil));
  }
}