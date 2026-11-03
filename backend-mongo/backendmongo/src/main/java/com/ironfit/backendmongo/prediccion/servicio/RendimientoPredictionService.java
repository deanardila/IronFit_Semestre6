package com.ironfit.backendmongo.prediccion.servicio;

import com.ironfit.backendmongo.modelo.planes.PlanEntrenamiento;
import com.ironfit.backendmongo.prediccion.dto.PrediccionResponse;
import com.ironfit.backendmongo.prediccion.weka.WekaModelLoader;
import com.ironfit.backendmongo.repositorio.planes.PlanEntrenamientoRepository;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RendimientoPredictionService {

    private final WekaModelLoader modelLoader;
    private final PlanEntrenamientoRepository planRepository;
    private final MongoTemplate mongoTemplate;

    public RendimientoPredictionService(
            WekaModelLoader modelLoader,
            PlanEntrenamientoRepository planRepository,
            MongoTemplate mongoTemplate
    ) {
        this.modelLoader = modelLoader;
        this.planRepository = planRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public PrediccionResponse predecir(String planId) throws Exception {

        PlanEntrenamiento plan = planRepository.findById(planId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe el plan: " + planId
                        )
                );

        String clienteId = plan.getClienteId();
        String objetivo = plan.getObjetivo();

        // ==========================================
        // 1. DÍAS DEL PLAN
        // ==========================================

        long diasEnPlan = 1;

        if (plan.getFechaInicio() != null &&
            plan.getFechaFin() != null) {

            diasEnPlan = Math.max(
                    1,
                    ChronoUnit.DAYS.between(
                            plan.getFechaInicio(),
                            plan.getFechaFin()
                    )
            );
        }

        // ==========================================
        // 2. ENTRENAMIENTOS
        // ==========================================

        Query queryEntrenamientos = Query.query(
                Criteria.where("planId").is(planId)
                        .and("clienteId").is(clienteId)
        ).with(
                Sort.by(
                        Sort.Direction.ASC,
                        "fecha"
                )
        );

        List<Document> entrenamientos =
                mongoTemplate.find(
                        queryEntrenamientos,
                        Document.class,
                        "entrenamientos_realizados"
                );

        int totalEntrenamientos =
                entrenamientos.size();

        double semanas =
                Math.max(diasEnPlan / 7.0, 1.0);

        double frecuenciaSemanal =
                totalEntrenamientos / semanas;

        // ==========================================
        // 3. PORCENTAJE PROMEDIO DE SESIÓN
        // ==========================================

        double sumaPorcentajeSesion = 0;
        int cantidadPorcentajes = 0;

        // ==========================================
        // 4. SERIES / REPETICIONES / CARGAS
        // ==========================================

        double sumaSeries = 0;
        int cantidadSeries = 0;

        double sumaRepeticiones = 0;
        int cantidadRepeticiones = 0;

        /*
         * Guardamos el primer peso registrado
         * de cada ejercicio.
         *
         * Eso reproduce la lógica del dataset:
         * promedio_peso_inicial.
         */
        Map<String, Double> primerPesoPorEjercicio =
                new HashMap<>();

        for (Document entrenamiento : entrenamientos) {

            Number porcentaje =
                    entrenamiento.get(
                            "porcentajeSesion",
                            Number.class
                    );

            if (porcentaje != null) {
                sumaPorcentajeSesion +=
                        porcentaje.doubleValue();

                cantidadPorcentajes++;
            }

            List<Document> ejercicios =
                    entrenamiento.getList(
                            "ejerciciosRealizados",
                            Document.class
                    );

            if (ejercicios == null) {
                continue;
            }

            for (Document ejercicio : ejercicios) {

                Number series =
                        ejercicio.get(
                                "seriesRealizadas",
                                Number.class
                        );

                if (series != null) {
                    sumaSeries +=
                            series.doubleValue();

                    cantidadSeries++;
                }

                Number repeticiones =
                        ejercicio.get(
                                "repeticionesRealizadas",
                                Number.class
                        );

                if (repeticiones != null) {
                    sumaRepeticiones +=
                            repeticiones.doubleValue();

                    cantidadRepeticiones++;
                }

                String ejercicioId =
                        ejercicio.getString(
                                "ejercicioId"
                        );

                Number peso =
                        ejercicio.get(
                                "pesoUsado",
                                Number.class
                        );

                if (
                        ejercicioId != null &&
                        peso != null &&
                        !primerPesoPorEjercicio
                                .containsKey(ejercicioId)
                ) {

                    primerPesoPorEjercicio.put(
                            ejercicioId,
                            peso.doubleValue()
                    );
                }
            }
        }

        double promedioPorcentajeSesion =
                cantidadPorcentajes > 0
                        ? sumaPorcentajeSesion
                            / cantidadPorcentajes
                        : 0;

        double promedioSeries =
                cantidadSeries > 0
                        ? sumaSeries
                            / cantidadSeries
                        : 0;

        double promedioRepeticiones =
                cantidadRepeticiones > 0
                        ? sumaRepeticiones
                            / cantidadRepeticiones
                        : 0;

        double promedioPesoInicial =
                primerPesoPorEjercicio
                        .values()
                        .stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0);

        // ==========================================
        // 5. EVALUACIONES FÍSICAS
        // ==========================================

        Query queryEvaluaciones = Query.query(
                Criteria.where("clienteId")
                        .is(clienteId)
        ).with(
                Sort.by(
                        Sort.Direction.ASC,
                        "fecha"
                )
        );

        List<Document> evaluaciones =
                mongoTemplate.find(
                        queryEvaluaciones,
                        Document.class,
                        "evaluaciones_fisicas"
                );

        int cantidadEvaluaciones =
                evaluaciones.size();

        double pesoCorporalInicial = 0;
        double grasaInicial = 0;

        double cinturaInicial = 0;
        double brazoInicial = 0;
        double pechoInicial = 0;
        double piernaInicial = 0;

        if (!evaluaciones.isEmpty()) {

            Document primera =
                    evaluaciones.get(0);

            pesoCorporalInicial =
                    numero(
                            primera,
                            "pesoCorporal"
                    );

            grasaInicial =
                    numero(
                            primera,
                            "porcentajeGraso"
                    );

            Document medidas =
                    primera.get(
                            "medidasCorporales",
                            Document.class
                    );

            if (medidas != null) {

                cinturaInicial =
                        numero(
                                medidas,
                                "cintura"
                        );

                brazoInicial =
                        numero(
                                medidas,
                                "brazo"
                        );

                pechoInicial =
                        numero(
                                medidas,
                                "pecho"
                        );

                piernaInicial =
                        numero(
                                medidas,
                                "pierna"
                        );
            }
        }

        // ==========================================
        // 6. CREAR INSTANCIA WEKA
        // ==========================================

        Instances estructura =
                crearEstructura();

        Instance instancia =
                new DenseInstance(
                        estructura.numAttributes()
                );

        instancia.setDataset(estructura);

        instancia.setValue(
                estructura.attribute("objetivo"),
                objetivo
        );

        instancia.setValue(
                estructura.attribute("dias_en_plan"),
                diasEnPlan
        );

        instancia.setValue(
                estructura.attribute(
                        "entrenamientos_realizados"
                ),
                totalEntrenamientos
        );

        instancia.setValue(
                estructura.attribute(
                        "frecuencia_semanal"
                ),
                frecuenciaSemanal
        );

        instancia.setValue(
                estructura.attribute(
                        "promedio_porcentaje_sesion"
                ),
                promedioPorcentajeSesion
        );

        instancia.setValue(
                estructura.attribute(
                        "promedio_series"
                ),
                promedioSeries
        );

        instancia.setValue(
                estructura.attribute(
                        "promedio_repeticiones"
                ),
                promedioRepeticiones
        );

        instancia.setValue(
                estructura.attribute(
                        "promedio_peso_inicial"
                ),
                promedioPesoInicial
        );

        instancia.setValue(
                estructura.attribute(
                        "cantidad_evaluaciones"
                ),
                cantidadEvaluaciones
        );

        instancia.setValue(
                estructura.attribute(
                        "peso_corporal_inicial"
                ),
                pesoCorporalInicial
        );

        instancia.setValue(
                estructura.attribute(
                        "grasa_inicial"
                ),
                grasaInicial
        );

        instancia.setValue(
                estructura.attribute(
                        "cintura_inicial"
                ),
                cinturaInicial
        );

        instancia.setValue(
                estructura.attribute(
                        "brazo_inicial"
                ),
                brazoInicial
        );

        instancia.setValue(
                estructura.attribute(
                        "pecho_inicial"
                ),
                pechoInicial
        );

        instancia.setValue(
                estructura.attribute(
                        "pierna_inicial"
                ),
                piernaInicial
        );

        instancia.setMissing(
                estructura.classIndex()
        );

        // ==========================================
        // 7. PREDICCIÓN
        // ==========================================

        Classifier modelo =
                modelLoader.getModeloRendimiento();

        double resultado =
                modelo.classifyInstance(
                        instancia
                );

        String prediccion =
                estructura
                        .classAttribute()
                        .value(
                                (int) resultado
                        );

        double[] distribucion =
                modelo.distributionForInstance(
                        instancia
                );

        Map<String, Double> probabilidades =
                new LinkedHashMap<>();

        for (
                int i = 0;
                i < distribucion.length;
                i++
        ) {

            String clase =
                    estructura
                            .classAttribute()
                            .value(i);

            probabilidades.put(
                    clase,
                    Math.round(
                            distribucion[i]
                                    * 10000.0
                    ) / 100.0
            );
        }

        return new PrediccionResponse(
                clienteId,
                planId,
                "RENDIMIENTO",
                prediccion,
                "RandomForest",
                probabilidades
        );
    }

    // ==============================================
    // ESTRUCTURA EXACTA DEL ARFF DE RENDIMIENTO
    // ==============================================

    private Instances crearEstructura() {

        ArrayList<Attribute> atributos =
                new ArrayList<>();

        ArrayList<String> objetivos =
                new ArrayList<>(
                        List.of(
                                "Bajar grasa",
                                "Ganar masa muscular",
                                "Incrementar fuerza máxima",
                                "Mejorar fuerza",
                                "Mejorar resistencia",
                                "Tonificación"
                        )
                );

        atributos.add(
                new Attribute(
                        "objetivo",
                        objetivos
                )
        );

        atributos.add(
                new Attribute(
                        "dias_en_plan"
                )
        );

        atributos.add(
                new Attribute(
                        "entrenamientos_realizados"
                )
        );

        atributos.add(
                new Attribute(
                        "frecuencia_semanal"
                )
        );

        atributos.add(
                new Attribute(
                        "promedio_porcentaje_sesion"
                )
        );

        atributos.add(
                new Attribute(
                        "promedio_series"
                )
        );

        atributos.add(
                new Attribute(
                        "promedio_repeticiones"
                )
        );

        atributos.add(
                new Attribute(
                        "promedio_peso_inicial"
                )
        );

        atributos.add(
                new Attribute(
                        "cantidad_evaluaciones"
                )
        );

        atributos.add(
                new Attribute(
                        "peso_corporal_inicial"
                )
        );

        atributos.add(
                new Attribute(
                        "grasa_inicial"
                )
        );

        atributos.add(
                new Attribute(
                        "cintura_inicial"
                )
        );

        atributos.add(
                new Attribute(
                        "brazo_inicial"
                )
        );

        atributos.add(
                new Attribute(
                        "pecho_inicial"
                )
        );

        atributos.add(
                new Attribute(
                        "pierna_inicial"
                )
        );

        ArrayList<String> clases =
                new ArrayList<>(
                        List.of(
                                "ALTO",
                                "BAJO",
                                "MEDIO"
                        )
                );

        atributos.add(
                new Attribute(
                        "nivel_rendimiento",
                        clases
                )
        );

        Instances estructura =
                new Instances(
                        "ironfit_rendimiento",
                        atributos,
                        0
                );

        estructura.setClassIndex(
                estructura.numAttributes() - 1
        );

        return estructura;
    }

    private double numero(
            Document documento,
            String campo
    ) {

        Number valor =
                documento.get(
                        campo,
                        Number.class
                );

        return valor == null
                ? 0
                : valor.doubleValue();
    }
}