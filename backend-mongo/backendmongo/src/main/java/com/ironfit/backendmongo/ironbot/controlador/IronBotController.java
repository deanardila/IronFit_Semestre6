package com.ironfit.backendmongo.ironbot.controlador;

import com.ironfit.backendmongo.ironbot.dto.IronBotResponse;
import com.ironfit.backendmongo.ironbot.servicio.IronBotService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/ironbot")
public class IronBotController {
    private final IronBotService service;
    public IronBotController(IronBotService service) { this.service=service; }
    @GetMapping("/entrenador/plan/{planId}")
    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR')")
    public IronBotResponse plan(@PathVariable String planId, Authentication auth) { return service.analizarPlan(planId, auth); }
    @GetMapping("/admin/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    public IronBotResponse admin(Authentication auth) { return service.resumenAdmin(auth); }
    @GetMapping("/cliente/resumen")
    @PreAuthorize("hasRole('CLIENTE')")
    public IronBotResponse cliente(Authentication auth) { return service.resumenCliente(auth); }
}
