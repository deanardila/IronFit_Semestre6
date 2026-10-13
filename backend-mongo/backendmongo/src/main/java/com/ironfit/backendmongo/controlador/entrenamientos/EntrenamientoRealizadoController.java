package com.ironfit.backendmongo.controlador.entrenamientos;

import com.ironfit.backendmongo.dto.entrenamientos.EntrenamientoRealizadoCrearRequest;
import com.ironfit.backendmongo.dto.entrenamientos.EntrenamientoRealizadoResponse;
import com.ironfit.backendmongo.servicio.entrenamientos.EntrenamientoRealizadoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entrenamientos-realizados")
public class EntrenamientoRealizadoController {

    private final EntrenamientoRealizadoService entrenamientoRealizadoService;

    public EntrenamientoRealizadoController(EntrenamientoRealizadoService entrenamientoRealizadoService) {
        this.entrenamientoRealizadoService = entrenamientoRealizadoService;
    }

    @PreAuthorize("hasRole('CLIENTE')")
    @PostMapping
    public EntrenamientoRealizadoResponse registrarEntrenamiento(
            Authentication authentication,
            @Valid @RequestBody EntrenamientoRealizadoCrearRequest request
    ) {
        return entrenamientoRealizadoService.registrarEntrenamiento(authentication, request);
    }

    @PreAuthorize("hasRole('CLIENTE')")
    @GetMapping("/mis-entrenamientos")
    public List<EntrenamientoRealizadoResponse> listarMisEntrenamientos(
            Authentication authentication
    ) {
        return entrenamientoRealizadoService.listarMisEntrenamientos(authentication);
    }

    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR')")
    @GetMapping("/cliente/{clienteId}")
    public List<EntrenamientoRealizadoResponse> listarPorCliente(
            Authentication authentication,
            @PathVariable String clienteId
    ) {
        return entrenamientoRealizadoService.listarPorCliente(authentication, clienteId);
    }
}