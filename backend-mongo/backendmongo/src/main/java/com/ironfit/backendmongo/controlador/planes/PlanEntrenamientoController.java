package com.ironfit.backendmongo.controlador.planes;

import com.ironfit.backendmongo.dto.comun.PaginaResponse;
import com.ironfit.backendmongo.dto.planes.PlanEntrenamientoActualizarRequest;
import com.ironfit.backendmongo.dto.planes.PlanEntrenamientoCrearRequest;
import com.ironfit.backendmongo.dto.planes.PlanEntrenamientoResponse;
import com.ironfit.backendmongo.servicio.planes.PlanEntrenamientoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/planes")
public class PlanEntrenamientoController {

    private final PlanEntrenamientoService planEntrenamientoService;

    public PlanEntrenamientoController(PlanEntrenamientoService planEntrenamientoService) {
        this.planEntrenamientoService = planEntrenamientoService;
    }

    // LISTAR - ADMIN, ENTRENADOR y CLIENTE
    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR','CLIENTE')")
    @GetMapping
    public List<PlanEntrenamientoResponse> listarPlanes(Authentication authentication) {
        return planEntrenamientoService.listarPlanes(authentication);
    }

    // LISTAR PAGINADO - ADMIN, ENTRENADOR y CLIENTE
    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR','CLIENTE')")
    @GetMapping("/paginado")
    public PaginaResponse<PlanEntrenamientoResponse> listarPlanesPaginados(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Boolean activo
    ) {
        return planEntrenamientoService.listarPlanesPaginados(
                authentication,
                page,
                size,
                buscar,
                activo
        );
    }

    // CREAR - solo ENTRENADOR
    @PreAuthorize("hasRole('ENTRENADOR')")
    @PostMapping
    public PlanEntrenamientoResponse crearPlan(
            Authentication authentication,
            @Valid @RequestBody PlanEntrenamientoCrearRequest request
    ) {
        return planEntrenamientoService.crearPlan(authentication, request);
    }

    // ACTUALIZAR - solo ENTRENADOR
    @PreAuthorize("hasRole('ENTRENADOR')")
    @PutMapping("/{id}")
    public PlanEntrenamientoResponse actualizarPlan(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody PlanEntrenamientoActualizarRequest request
    ) {
        return planEntrenamientoService.actualizarPlan(authentication, id, request);
    }

    // CAMBIAR ESTADO - solo ENTRENADOR
    @PreAuthorize("hasRole('ENTRENADOR')")
    @PatchMapping("/{id}/estado")
    public PlanEntrenamientoResponse cambiarEstado(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam Boolean activo
    ) {
        return planEntrenamientoService.cambiarEstado(authentication, id, activo);
    }

    // VER DETALLE - ADMIN, ENTRENADOR o CLIENTE
    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR','CLIENTE')")
    @GetMapping("/{id}")
    public PlanEntrenamientoResponse obtenerPlan(
            Authentication authentication,
            @PathVariable String id
    ) {
        return planEntrenamientoService.obtenerPlan(authentication, id);
    }
}