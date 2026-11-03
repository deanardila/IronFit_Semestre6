package com.ironfit.backendmongo.prediccion.servicio;

import com.ironfit.backendmongo.prediccion.dto.PrediccionPlanResponse;
import com.ironfit.backendmongo.prediccion.dto.PrediccionResponse;
import org.springframework.stereotype.Service;

@Service
public class PrediccionPlanService {

    private final AdherenciaPredictionService adherenciaService;
    private final RendimientoPredictionService rendimientoService;

    public PrediccionPlanService(
            AdherenciaPredictionService adherenciaService,
            RendimientoPredictionService rendimientoService
    ) {
        this.adherenciaService = adherenciaService;
        this.rendimientoService = rendimientoService;
    }

    public PrediccionPlanResponse predecirPlan(
            String planId
    ) throws Exception {

        PrediccionResponse adherencia =
                adherenciaService.predecir(planId);

        PrediccionResponse rendimiento =
                rendimientoService.predecir(planId);

        return new PrediccionPlanResponse(
                adherencia.getClienteId(),
                planId,
                adherencia,
                rendimiento
        );
    }
}