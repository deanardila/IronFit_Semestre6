package com.ironfit.backendmongo.controlador.seguridad.rutinas;

import com.ironfit.backendmongo.dto.rutinas.RutinaRequest;
import com.ironfit.backendmongo.dto.rutinas.RutinaResponse;
import com.ironfit.backendmongo.servicio.rutinas.RutinaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rutinas")
public class RutinaController {

    private final RutinaService rutinaService;

    public RutinaController(RutinaService rutinaService) {
        this.rutinaService = rutinaService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR','CLIENTE')")
    @GetMapping("/plan/{planId}")
    public List<RutinaResponse> listarRutinasPorPlan(
            Authentication authentication,
            @PathVariable String planId
    ) {
        return rutinaService.listarRutinasPorPlan(authentication, planId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR')")
    @GetMapping("/plan/{planId}/gestion")
    public List<RutinaResponse> listarRutinasPorPlanGestion(
            Authentication authentication,
            @PathVariable String planId
    ) {
        return rutinaService.listarRutinasPorPlanGestion(authentication, planId);
    }

    @PreAuthorize("hasRole('ENTRENADOR')")
    @PostMapping
    public RutinaResponse crearRutina(
            Authentication authentication,
            @RequestBody RutinaRequest request
    ) {
        return rutinaService.crearRutina(authentication, request);
    }

    @PreAuthorize("hasRole('ENTRENADOR')")
    @PutMapping("/{id}")
    public RutinaResponse actualizarRutina(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody RutinaRequest request
    ) {
        return rutinaService.actualizarRutina(authentication, id, request);
    }

    @PreAuthorize("hasRole('ENTRENADOR')")
    @PatchMapping("/{id}/estado")
    public RutinaResponse cambiarEstadoRutina(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam Boolean activo
    ) {
        return rutinaService.cambiarEstadoRutina(authentication, id, activo);
    }
}