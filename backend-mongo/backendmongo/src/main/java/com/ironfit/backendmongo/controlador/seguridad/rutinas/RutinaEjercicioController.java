package com.ironfit.backendmongo.controlador.seguridad.rutinas;

import com.ironfit.backendmongo.dto.rutinas.RutinaEjercicioRequest;
import com.ironfit.backendmongo.dto.rutinas.RutinaEjercicioResponse;
import com.ironfit.backendmongo.servicio.rutinas.RutinaEjercicioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rutina-ejercicios")
public class RutinaEjercicioController {

    private final RutinaEjercicioService rutinaEjercicioService;

    public RutinaEjercicioController(RutinaEjercicioService rutinaEjercicioService) {
        this.rutinaEjercicioService = rutinaEjercicioService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR','CLIENTE')")
    @GetMapping("/rutina/{rutinaId}")
    public List<RutinaEjercicioResponse> listarEjerciciosPorRutina(
            Authentication authentication,
            @PathVariable String rutinaId
    ) {
        return rutinaEjercicioService.listarEjerciciosPorRutina(authentication, rutinaId);
    }

    @PreAuthorize("hasRole('ENTRENADOR')")
    @PostMapping
    public RutinaEjercicioResponse agregarEjercicioARutina(
            Authentication authentication,
            @RequestBody RutinaEjercicioRequest request
    ) {
        return rutinaEjercicioService.agregarEjercicioARutina(authentication, request);
    }

    @PreAuthorize("hasRole('ENTRENADOR')")
    @PutMapping("/{id}")
    public RutinaEjercicioResponse actualizarEjercicioDeRutina(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody RutinaEjercicioRequest request
    ) {
        return rutinaEjercicioService.actualizarEjercicioDeRutina(authentication, id, request);
    }

    @PreAuthorize("hasRole('ENTRENADOR')")
    @DeleteMapping("/{id}")
    public void eliminarEjercicioDeRutina(
            Authentication authentication,
            @PathVariable String id
    ) {
        rutinaEjercicioService.eliminarEjercicioDeRutina(authentication, id);
    }
}