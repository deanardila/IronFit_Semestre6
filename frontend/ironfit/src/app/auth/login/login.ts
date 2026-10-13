import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { Auth } from '../auth';
import { UsuarioResumen, Usuarios } from '../../modules/admin/usuarios';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly perfilCacheKey = 'miPerfilCache';

  usuario: string = '';
  clave: string = '';
  mensajeError: string = '';
  mostrarPassword = false;

  constructor(
    private auth: Auth,
    private router: Router,
    private usuariosApi: Usuarios
  ) {}

  login() {
    if (!this.usuario || !this.clave) {
      this.mensajeError = 'Debe ingresar correo y contraseña';
      return;
    }

    const credenciales = {
      correo: this.usuario,
      password: this.clave
    };

    console.log('Credenciales enviadas:', credenciales);

    this.auth.login(credenciales).subscribe({
      next: (res: any) => {
        console.log('JSON recibido:', res);

        const rolDetectado = Array.isArray(res.roles) ? res.roles[0] : res.rol;

        const token = res.token || res.accessToken || res.jwt || null;
        console.log('Token detectado:', token);
        if (!token) {
          this.mensajeError = 'No se recibió token de autenticación';
          return;
        }

        this.auth.setSession(res.token, rolDetectado);

        const nombreDetectado =
          res.usuario?.nombres ??
          res.nombres ??
          res.nombre ??
          res.username ??
          this.usuario;

        console.log('Nombre detectado para mostrar:', nombreDetectado);
        localStorage.setItem('nombreUsuario', nombreDetectado);

        this.sembrarPerfilEnCache(res, rolDetectado, nombreDetectado);
        this.refrescarPerfilEnSegundoPlano();

        switch (rolDetectado) {
          case 'ADMIN':
            this.router.navigate(['/admin']);
            break;
          case 'ENTRENADOR':
            this.router.navigate(['/entrenador']);
            break;
          case 'CLIENTE':
            this.router.navigate(['/cliente']);
            break;
          default:
            this.mensajeError = 'Rol desconocido';
            break;
        }
      },
      error: err => {
        console.error(err);
        this.mensajeError = 'Credenciales incorrectas';
      }
    });
  }

  togglePassword() {
    this.mostrarPassword = !this.mostrarPassword;
  }

  private sembrarPerfilEnCache(res: any, rolDetectado: string, nombreDetectado: string): void {
    const usuarioRes = res?.usuario ?? {};
    const rolesDetectados = Array.isArray(res?.roles)
      ? res.roles
      : rolDetectado
      ? [rolDetectado]
      : [];

    const perfilInicial: UsuarioResumen = {
      id: String(usuarioRes.id ?? ''),
      tipoDoc: String(usuarioRes.tipoDoc ?? ''),
      nroDoc: String(usuarioRes.nroDoc ?? ''),
      numDoc: String(usuarioRes.numDoc ?? usuarioRes.nroDoc ?? ''),
      nombres: String(usuarioRes.nombres ?? nombreDetectado ?? ''),
      apellidos: String(usuarioRes.apellidos ?? ''),
      telefono: usuarioRes.telefono ?? '',
      correo: usuarioRes.correo ?? this.usuario,
      activo: usuarioRes.activo ?? true,
      roles: rolesDetectados
    };

    localStorage.setItem(this.perfilCacheKey, JSON.stringify(perfilInicial));
  }

  private refrescarPerfilEnSegundoPlano(): void {
    this.usuariosApi.getMiPerfil().subscribe({
      next: (perfil: UsuarioResumen) => {
        localStorage.setItem(this.perfilCacheKey, JSON.stringify(perfil));
      },
      error: () => {
        // Silencioso: mantenemos el cache inicial si el refresh falla.
      }
    });
  }
}