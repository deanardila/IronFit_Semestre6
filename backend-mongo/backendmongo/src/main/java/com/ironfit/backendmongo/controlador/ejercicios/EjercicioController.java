package com.ironfit.backendmongo.controlador.ejercicios;

import com.ironfit.backendmongo.dto.comun.PaginaResponse;
import com.ironfit.backendmongo.dto.ejercicios.EjercicioActualizarRequest;
import com.ironfit.backendmongo.dto.ejercicios.EjercicioCrearRequest;
import com.ironfit.backendmongo.dto.ejercicios.EjercicioResponse;
import com.ironfit.backendmongo.servicio.ejercicios.EjercicioService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ejercicios")
public class EjercicioController {

    private final EjercicioService ejercicioService;

    public EjercicioController(EjercicioService ejercicioService) {
        this.ejercicioService = ejercicioService;
    }

    // LISTAR
    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR')")
    @GetMapping
    public List<EjercicioResponse> listarEjercicios() {
        return ejercicioService.listarEjercicios();
    }

    // LISTAR PAGINADO
    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR')")
    @GetMapping("/paginado")
    public PaginaResponse<EjercicioResponse> listarEjerciciosPaginados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String grupoMuscular,
            @RequestParam(required = false) Boolean activo
    ) {
        return ejercicioService.listarEjerciciosPaginados(
                page,
                size,
                buscar,
                categoria,
                grupoMuscular,
                activo
        );
    }

    // CREAR
    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR')")
    @PostMapping
    public EjercicioResponse crearEjercicio(@Valid @RequestBody EjercicioCrearRequest request) {
        return ejercicioService.crearEjercicio(request);
    }

    // ACTUALIZAR
    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR')")
    @PutMapping("/{id}")
    public EjercicioResponse actualizarEjercicio(
            @PathVariable String id,
            @Valid @RequestBody EjercicioActualizarRequest request
    ) {
        return ejercicioService.actualizarEjercicio(id, request);
    }

    // CAMBIAR ESTADO (ACTIVO/INACTIVO)
    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR')")
    @PatchMapping("/{id}/estado")
    public void cambiarEstado(
            @PathVariable String id,
            @RequestParam Boolean activo
    ) {
        ejercicioService.cambiarEstado(id, activo);
    }
}