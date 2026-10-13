package com.ironfit.backendmongo.servicio.seguridad;

import com.ironfit.backendmongo.dto.seguridad.LoginRequest;
import com.ironfit.backendmongo.dto.seguridad.LoginResponse;
import com.ironfit.backendmongo.modelo.seguridad.UserDocument;
import com.ironfit.backendmongo.repositorio.seguridad.UserRepository;
import com.ironfit.backendmongo.seguridad.jwt.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository repositorioUsuario;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository repositorioUsuario, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.repositorioUsuario = repositorioUsuario;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest solicitud) {
    System.out.println("CORREO RECIBIDO: " + solicitud.getCorreo());
    UserDocument usuario = repositorioUsuario.findByCorreo(solicitud.getCorreo())
            .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

    if (!usuario.isActivo()) {
        throw new RuntimeException("Usuario inactivo");
    }

    if (!passwordEncoder.matches(solicitud.getPassword(), usuario.getPasswordHash())) {
        throw new RuntimeException("Credenciales inválidas");
    }

    String token = jwtService.generarToken(
            usuario.getId(),
            usuario.getCorreo(),
            usuario.getRoles()
    );

    return new LoginResponse(token, usuario.getCorreo(), usuario.getRoles());
    }
}