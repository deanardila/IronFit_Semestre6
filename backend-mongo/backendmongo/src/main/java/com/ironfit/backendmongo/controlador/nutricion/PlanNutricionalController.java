package com.ironfit.backendmongo.controlador.nutricion;

import com.ironfit.backendmongo.dto.nutricion.PlanNutricionalRequest;
import com.ironfit.backendmongo.dto.nutricion.PlanNutricionalResponse;
import com.ironfit.backendmongo.servicio.nutricion.PlanNutricionalService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nutricion")
public class PlanNutricionalController {

    private final PlanNutricionalService nutricionalService;

    public PlanNutricionalController(PlanNutricionalService nutricionalService) {
        this.nutricionalService = nutricionalService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR','CLIENTE')")
    @GetMapping
    public List<PlanNutricionalResponse> listar(Authentication authentication) {
        return nutricionalService.listar(authentication);
    }

    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR','CLIENTE')")
    @GetMapping("/plan/{planId}")
    public PlanNutricionalResponse obtenerPorPlan(
            Authentication authentication,
            @PathVariable String planId
    ) {
        return nutricionalService.obtenerPorPlan(authentication, planId);
    }

    @PreAuthorize("hasRole('ENTRENADOR')")
    @PostMapping
    public PlanNutricionalResponse crear(
            Authentication authentication,
            @Valid @RequestBody PlanNutricionalRequest request
    ) {
        return nutricionalService.crear(authentication, request);
    }

    @PreAuthorize("hasRole('ENTRENADOR')")
    @PutMapping("/{id}")
    public PlanNutricionalResponse actualizar(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody PlanNutricionalRequest request
    ) {
        return nutricionalService.actualizar(authentication, id, request);
    }
}
