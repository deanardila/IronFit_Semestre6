package com.ironfit.backendmongo.seguridad.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();
        if (path.startsWith("/api/auth") || request.getMethod().equalsIgnoreCase("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtService.esValido(token)) {
                Jws<Claims> parsed = jwtService.parsear(token);
                Claims claims = parsed.getBody();

                String correo = claims.get("correo", String.class);
                List<?> rolesRaw = claims.get("roles", List.class);

                var authorities = rolesRaw == null ? List.<GrantedAuthority>of() :
                        rolesRaw.stream()
                                .map(Object::toString)
                                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                                .collect(Collectors.toList());

                var auth = UsernamePasswordAuthenticationToken.authenticated(correo, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                System.out.println("JWT FILTER -> " 
                    + request.getMethod()
                    + " "
                    + request.getServletPath());

                System.out.println("Usuario autenticado: " + correo);
                System.out.println("Roles token: " + rolesRaw);
                System.out.println("Authorities finales: " + authorities);
            }
        }

        filterChain.doFilter(request, response);
    }
}