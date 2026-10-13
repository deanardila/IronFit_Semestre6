package com.ironfit.backendmongo.controlador.dashboard;

import com.ironfit.backendmongo.dto.dashboard.DashboardAdminResponse;
import com.ironfit.backendmongo.dto.dashboard.DashboardClienteResponse;
import com.ironfit.backendmongo.dto.dashboard.DashboardEntrenadorResponse;
import com.ironfit.backendmongo.servicio.dashboard.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

        private final DashboardService dashboardService;

        public DashboardController(DashboardService dashboardService) {
                this.dashboardService = dashboardService;
        }

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/admin")
        public DashboardAdminResponse obtenerDashboardAdmin(Authentication authentication) {
                return dashboardService.obtenerDashboardAdmin(authentication);
        }

        @PreAuthorize("hasRole('ENTRENADOR')")
        @GetMapping("/entrenador")
        public DashboardEntrenadorResponse obtenerDashboardEntrenador(Authentication authentication) {
                return dashboardService.obtenerDashboardEntrenador(authentication);
        }

        @PreAuthorize("hasRole('CLIENTE')")
        @GetMapping("/cliente")
        public DashboardClienteResponse obtenerDashboardCliente(Authentication authentication) {
                return dashboardService.obtenerDashboardCliente(authentication);
        }
}