package com.ironfit.backendmongo.configuracion;

import com.ironfit.backendmongo.seguridad.jwt.JwtAuthFilter;
import com.ironfit.backendmongo.seguridad.jwt.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));               // acepta cualquier origen
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));                      // acepta cualquier header
        config.setExposedHeaders(List.of("Authorization"));          // exponer el token al frontend
        config.setAllowCredentials(false);                           // obligatorio con patrones '*'
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }


    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtService jwtService,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.disable())
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Rutas públicas
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/test/**").permitAll()
                .requestMatchers("/api/usuarios/me").authenticated()
                .requestMatchers("/api/usuarios/clientes/buscar")
                    .hasAnyRole("ADMIN", "ENTRENADOR")
                .requestMatchers("/api/usuarios/**").hasRole("ADMIN")

                // Rutas de ejercicios
                .requestMatchers(HttpMethod.GET, "/api/ejercicios/**")
                    .hasAnyRole("ADMIN", "ENTRENADOR")
                .requestMatchers(HttpMethod.POST, "/api/ejercicios/**")
                    .hasAnyRole("ADMIN", "ENTRENADOR")
                .requestMatchers(HttpMethod.PUT, "/api/ejercicios/**")
                    .hasAnyRole("ADMIN", "ENTRENADOR")
                .requestMatchers(HttpMethod.PATCH, "/api/ejercicios/**")
                    .hasAnyRole("ADMIN", "ENTRENADOR")

                // Rutas de planes de entrenamiento
                .requestMatchers(HttpMethod.GET, "/api/planes/**")
                    .hasAnyRole("ADMIN", "ENTRENADOR", "CLIENTE")
                .requestMatchers(HttpMethod.POST, "/api/planes/**")
                    .hasRole("ENTRENADOR")
                .requestMatchers(HttpMethod.PUT, "/api/planes/**")
                    .hasRole("ENTRENADOR")
                .requestMatchers(HttpMethod.PATCH, "/api/planes/**")
                    .hasRole("ENTRENADOR")

                // Rutas de rutinas
                .requestMatchers(HttpMethod.GET, "/api/rutinas/**")
                    .hasAnyRole("ADMIN", "ENTRENADOR", "CLIENTE")
                .requestMatchers(HttpMethod.POST, "/api/rutinas/**")
                    .hasRole("ENTRENADOR")
                .requestMatchers(HttpMethod.PUT, "/api/rutinas/**")
                    .hasRole("ENTRENADOR")
                .requestMatchers(HttpMethod.PATCH, "/api/rutinas/**")
                    .hasRole("ENTRENADOR")

                // Rutas de ejercicios en rutinas
                .requestMatchers(HttpMethod.GET, "/api/rutina-ejercicios/**")
                    .hasAnyRole("ADMIN", "ENTRENADOR", "CLIENTE")
                .requestMatchers(HttpMethod.POST, "/api/rutina-ejercicios/**")
                    .hasRole("ENTRENADOR")
                .requestMatchers(HttpMethod.PUT, "/api/rutina-ejercicios/**")
                    .hasRole("ENTRENADOR")
                .requestMatchers(HttpMethod.DELETE, "/api/rutina-ejercicios/**")
                    .hasRole("ENTRENADOR")

                // Rutas de entrenamientos realizados
                .requestMatchers(HttpMethod.POST, "/api/entrenamientos-realizados/**")
                    .hasRole("CLIENTE")
                .requestMatchers(HttpMethod.GET, "/api/entrenamientos-realizados/mis-entrenamientos")
                    .hasRole("CLIENTE")
                .requestMatchers(HttpMethod.GET, "/api/entrenamientos-realizados/cliente/**")
                    .hasAnyRole("ADMIN", "ENTRENADOR")


                .anyRequest().authenticated()
            )
            .addFilterBefore(
                new JwtAuthFilter(jwtService),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}