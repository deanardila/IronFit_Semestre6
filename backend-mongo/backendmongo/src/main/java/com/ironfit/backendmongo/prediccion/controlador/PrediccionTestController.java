package com.ironfit.backendmongo.prediccion.controlador;

import com.ironfit.backendmongo.prediccion.servicio.AdherenciaPredictionService;
import com.ironfit.backendmongo.prediccion.servicio.PrediccionPlanService;
import com.ironfit.backendmongo.prediccion.servicio.RendimientoPredictionService;
import com.ironfit.backendmongo.prediccion.weka.WekaModelLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/predicciones")
public class PrediccionTestController {

    private final WekaModelLoader modelLoader;
    private final AdherenciaPredictionService adherenciaService;
    private final RendimientoPredictionService rendimientoService;
    private final PrediccionPlanService prediccionPlanService;

    public PrediccionTestController(
            WekaModelLoader modelLoader,
            AdherenciaPredictionService adherenciaService,
            RendimientoPredictionService rendimientoService,
            PrediccionPlanService prediccionPlanService
    ) {
        this.modelLoader = modelLoader;
        this.adherenciaService = adherenciaService;
        this.rendimientoService = rendimientoService;
        this.prediccionPlanService = prediccionPlanService;
    }

    @GetMapping("/test-modelos")
    public ResponseEntity<Map<String, Object>> testModelos() {

        Map<String, Object> respuesta =
                new LinkedHashMap<>();

        respuesta.put(
                "adherencia",
                modelLoader.modeloAdherenciaDisponible()
                        ? "OK"
                        : "ERROR"
        );

        respuesta.put(
                "modeloAdherencia",
                modelLoader.getModeloAdherencia()
                        .getClass()
                        .getSimpleName()
        );

        respuesta.put(
                "rendimiento",
                modelLoader.modeloRendimientoDisponible()
                        ? "OK"
                        : "ERROR"
        );

        respuesta.put(
                "modeloRendimiento",
                modelLoader.getModeloRendimiento()
                        .getClass()
                        .getSimpleName()
        );

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/adherencia/plan/{planId}")
    public ResponseEntity<?> predecirAdherencia(
            @PathVariable String planId
    ) {

        try {

            return ResponseEntity.ok(
                    adherenciaService.predecir(planId)
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }

    @GetMapping("/rendimiento/plan/{planId}")
    public ResponseEntity<?> predecirRendimiento(
            @PathVariable String planId
    ) {

        try {

            return ResponseEntity.ok(
                    rendimientoService.predecir(planId)
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }

    @GetMapping("/plan/{planId}")
    public ResponseEntity<?> predecirPlan(
            @PathVariable String planId
    ) {

        try {

            return ResponseEntity.ok(
                    prediccionPlanService.predecirPlan(planId)
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }
}
