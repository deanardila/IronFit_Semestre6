package com.ironfit.backendmongo.controlador.seguridad;

import com.ironfit.backendmongo.dto.seguridad.ChangeStatusRequest;
import com.ironfit.backendmongo.dto.seguridad.RegisterRequest;
import com.ironfit.backendmongo.dto.seguridad.UserResponse;
import com.ironfit.backendmongo.servicio.seguridad.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.ironfit.backendmongo.dto.comun.PaginaResponse;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UserController {

    private final UserService servicioUsuario;

    public UserController(UserService servicioUsuario) {
        this.servicioUsuario = servicioUsuario;
    }

    @GetMapping("/resumen")
    public ResponseEntity<List<UserResponse>> listarResumen() {
        return ResponseEntity.ok(servicioUsuario.listarUsuarios());
    }

    @GetMapping("/paginado")
    public ResponseEntity<PaginaResponse<UserResponse>> listarUsuariosPaginados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo
    ) {
        return ResponseEntity.ok(
                servicioUsuario.listarUsuariosPaginados(page, size, buscar, rol, activo)
        );
    }

    @PostMapping("/crear")
    public ResponseEntity<UserResponse> crearUsuario(@RequestBody RegisterRequest solicitud) {
        return ResponseEntity.ok(servicioUsuario.crearUsuario(solicitud));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> obtenerUsuarioActual(Authentication authentication) {
        String correo = authentication.getName();
        return ResponseEntity.ok(servicioUsuario.buscarPorCorreo(correo));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> actualizarMiPerfil(
            Authentication authentication,
            @RequestBody UserResponse solicitud
    ) {
        String correo = authentication.getName();
        return ResponseEntity.ok(servicioUsuario.actualizarMiPerfil(correo, solicitud));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<String> cambiarMiPassword(
            Authentication authentication,
            @RequestBody String nuevaPassword
    ) {
        String correo = authentication.getName();
        servicioUsuario.cambiarMiPassword(correo, nuevaPassword);
        return ResponseEntity.ok("Contraseña actualizada correctamente");
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<UserResponse> cambiarEstado(
            @PathVariable String id,
            @RequestBody ChangeStatusRequest solicitud
    ) {
        return ResponseEntity.ok(servicioUsuario.cambiarEstado(id, solicitud.isActivo()));
    }

    @PatchMapping("/{id}/resetear-password")
    public ResponseEntity<String> resetearPassword(@PathVariable String id) {
        servicioUsuario.resetearPassword(id);
        return ResponseEntity.ok("Contraseña restablecida correctamente");
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listarUsuarios() {
        return ResponseEntity.ok(servicioUsuario.listarUsuarios());
    }

    @GetMapping("/clientes/buscar")
    public ResponseEntity<List<UserResponse>> buscarClientes(
            @RequestParam String texto
    ) {
        return ResponseEntity.ok(servicioUsuario.buscarClientes(texto));
    }
}