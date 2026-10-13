package com.ironfit.backendmongo.configuracion;

import com.ironfit.backendmongo.modelo.seguridad.RoleName;
import com.ironfit.backendmongo.modelo.seguridad.UserDocument;
import com.ironfit.backendmongo.repositorio.seguridad.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class SeedAdmin {

    @Bean
    CommandLineRunner seed(UserRepository repositorioUsuario, PasswordEncoder passwordEncoder) {
        return args -> {
            crearSiNoExiste(repositorioUsuario, passwordEncoder,
                    "admin@ironfit.com", "admin123", List.of(RoleName.ADMIN),
                    "Admin", "Principal");

            crearSiNoExiste(repositorioUsuario, passwordEncoder,
                    "entrenador@ironfit.com", "entrenador123", List.of(RoleName.ENTRENADOR),
                    "Carlos", "Entrenador");

            crearSiNoExiste(repositorioUsuario, passwordEncoder,
                    "cliente@ironfit.com", "cliente123", List.of(RoleName.CLIENTE),
                    "Juan", "Cliente");
            
            System.out.println("USUARIOS EN BD:");
            repositorioUsuario.findAll().forEach(u ->
                    System.out.println(u.getCorreo() + " | " + u.getRoles()));
        };
    }

    private void crearSiNoExiste(UserRepository repositorioUsuario, PasswordEncoder passwordEncoder,
                                    String correo, String password, List<String> roles,
                                    String nombres, String apellidos) {

        if (!repositorioUsuario.existsByCorreo(correo)) {
            UserDocument usuario = new UserDocument();
            usuario.setCorreo(correo);
            usuario.setPasswordHash(passwordEncoder.encode(password));
            usuario.setRoles(roles);
            usuario.setActivo(true);
            usuario.setNombres(nombres);
            usuario.setApellidos(apellidos);
            usuario.setTelefono(null);
            usuario.setTipoDoc(null);
            usuario.setNumDoc(null);
            repositorioUsuario.save(usuario);
            System.out.println("Usuario creado: " + correo + " / " + password);
        }
    }
}