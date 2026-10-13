package com.ironfit.backendmongo.controlador.asistencias;

import com.ironfit.backendmongo.dto.asistencias.AsistenciaReporteResponse;
import com.ironfit.backendmongo.dto.comun.PaginaResponse;
import com.ironfit.backendmongo.servicio.asistencias.AsistenciaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/asistencias")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR')")
    @GetMapping("/reportes")
    public List<AsistenciaReporteResponse> generarReporte(
            Authentication authentication,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,

            @RequestParam(required = false)
            String clienteId,

            @RequestParam(required = false)
            String entrenadorId,

            @RequestParam(required = false)
            String estado
    ) {
        return asistenciaService.generarReporte(
                authentication,
                fechaInicio,
                fechaFin,
                clienteId,
                entrenadorId,
                estado
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR')")
    @GetMapping("/reportes/paginado")
    public PaginaResponse<AsistenciaReporteResponse> generarReportePaginado(
            Authentication authentication,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size,

            @RequestParam(required = false)
            String buscar,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,

            @RequestParam(required = false)
            String clienteId,

            @RequestParam(required = false)
            String entrenadorId,

            @RequestParam(required = false)
            String estado
    ) {
        return asistenciaService.generarReportePaginado(
                authentication,
                page,
                size,
                buscar,
                fechaInicio,
                fechaFin,
                clienteId,
                entrenadorId,
                estado
        );
    }
}