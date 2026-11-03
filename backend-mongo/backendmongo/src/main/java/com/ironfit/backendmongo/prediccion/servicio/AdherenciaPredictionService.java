package com.ironfit.backendmongo.prediccion.servicio;

import com.ironfit.backendmongo.modelo.planes.PlanEntrenamiento;
import com.ironfit.backendmongo.prediccion.dto.PrediccionResponse;
import com.ironfit.backendmongo.prediccion.weka.WekaModelLoader;
import com.ironfit.backendmongo.repositorio.planes.PlanEntrenamientoRepository;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdherenciaPredictionService {

    private final WekaModelLoader modelLoader;
    private final PlanEntrenamientoRepository planRepository;
    private final MongoTemplate mongoTemplate;

    public AdherenciaPredictionService(
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

        // ==========================================
        // 1. VARIABLES DEL PLAN
        // ==========================================

        String objetivo = plan.getObjetivo();

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
        // 2. RUTINAS
        // ==========================================

        long rutinasAsignadas = mongoTemplate.count(
                Query.query(
                        Criteria.where("planId").is(planId)
                ),
                "rutinas"
        );

        // ==========================================
        // 3. ASISTENCIAS / SESIONES REGISTRADAS
        // ==========================================

        Query queryAsistencias = Query.query(
                Criteria.where("planId").is(planId)
                        .and("clienteId").is(clienteId)
        );

        long sesionesRegistradas = mongoTemplate.count(
                queryAsistencias,
                "asistencias"
        );

        // ==========================================
        // 4. ENTRENAMIENTOS
        // ==========================================

        Query queryEntrenamientos = Query.query(
                Criteria.where("planId").is(planId)
                        .and("clienteId").is(clienteId)
        );

        List<Document> entrenamientos =
                mongoTemplate.find(
                        queryEntrenamientos,
                        Document.class,
                        "entrenamientos_realizados"
                );

        int entrenamientosRealizados =
                entrenamientos.size();

        double semanas =
                Math.max(diasEnPlan / 7.0, 1.0);

        double frecuenciaSemanal =
                entrenamientosRealizados / semanas;

        int ejerciciosRealizados = 0;
        int ejerciciosCompletados = 0;

        double sumaPesos = 0;
        int cantidadPesos = 0;

        for (Document entrenamiento : entrenamientos) {

            List<Document> ejercicios =
                    entrenamiento.getList(
                            "ejerciciosRealizados",
                            Document.class
                    );

            if (ejercicios == null) {
                continue;
            }

            ejerciciosRealizados += ejercicios.size();

            for (Document ejercicio : ejercicios) {

                Boolean completado =
                        ejercicio.getBoolean("completado");

                if (Boolean.TRUE.equals(completado)) {
                    ejerciciosCompletados++;
                }

                Number peso =
                        ejercicio.get(
                                "pesoUsado",
                                Number.class
                        );

                if (peso != null) {
                    sumaPesos += peso.doubleValue();
                    cantidadPesos++;
                }
            }
        }

        double promedioPesoUsado =
                cantidadPesos > 0
                        ? sumaPesos / cantidadPesos
                        : 0;

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

        double pesoInicial = 0;
        double pesoFinal = 0;
        double variacionPeso = 0;

        double imcInicial = 0;
        double imcFinal = 0;
        double variacionImc = 0;

        if (!evaluaciones.isEmpty()) {

            Document primera =
                    evaluaciones.get(0);

            Document ultima =
                    evaluaciones.get(
                            evaluaciones.size() - 1
                    );

            pesoInicial =
                    numero(
                            primera,
                            "pesoCorporal"
                    );

            pesoFinal =
                    numero(
                            ultima,
                            "pesoCorporal"
                    );

            variacionPeso =
                    pesoFinal - pesoInicial;

            imcInicial =
                    numero(
                            primera,
                            "imc"
                    );

            imcFinal =
                    numero(
                            ultima,
                            "imc"
                    );

            variacionImc =
                    imcFinal - imcInicial;
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
                estructura.attribute("rutinas_asignadas"),
                rutinasAsignadas
        );

        instancia.setValue(
                estructura.attribute("sesiones_registradas"),
                sesionesRegistradas
        );

        instancia.setValue(
                estructura.attribute(
                        "entrenamientos_realizados"
                ),
                entrenamientosRealizados
        );

        instancia.setValue(
                estructura.attribute("frecuencia_semanal"),
                frecuenciaSemanal
        );

        instancia.setValue(
                estructura.attribute(
                        "ejercicios_realizados"
                ),
                ejerciciosRealizados
        );

        instancia.setValue(
                estructura.attribute(
                        "ejercicios_completados"
                ),
                ejerciciosCompletados
        );

        instancia.setValue(
                estructura.attribute(
                        "promedio_peso_usado"
                ),
                promedioPesoUsado
        );

        instancia.setValue(
                estructura.attribute(
                        "cantidad_evaluaciones"
                ),
                cantidadEvaluaciones
        );

        instancia.setValue(
                estructura.attribute("peso_inicial"),
                pesoInicial
        );

        instancia.setValue(
                estructura.attribute("peso_final"),
                pesoFinal
        );

        instancia.setValue(
                estructura.attribute("variacion_peso"),
                variacionPeso
        );

        instancia.setValue(
                estructura.attribute("imc_inicial"),
                imcInicial
        );

        instancia.setValue(
                estructura.attribute("imc_final"),
                imcFinal
        );

        instancia.setValue(
                estructura.attribute("variacion_imc"),
                variacionImc
        );

        instancia.setMissing(
                estructura.classIndex()
        );

        // ==========================================
        // 7. PREDICCIÓN
        // ==========================================

        Classifier modelo =
                modelLoader.getModeloAdherencia();

        double resultado =
                modelo.classifyInstance(instancia);

        String prediccion =
                estructura.classAttribute()
                        .value((int) resultado);

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
                "ADHERENCIA",
                prediccion,
                "J48",
                probabilidades
        );
    }

    // ==============================================
    // ESTRUCTURA EXACTA UTILIZADA EN EL ARFF
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
                new Attribute("dias_en_plan")
        );

        atributos.add(
                new Attribute("rutinas_asignadas")
        );

        atributos.add(
                new Attribute("sesiones_registradas")
        );

        atributos.add(
                new Attribute(
                        "entrenamientos_realizados"
                )
        );

        atributos.add(
                new Attribute("frecuencia_semanal")
        );

        atributos.add(
                new Attribute(
                        "ejercicios_realizados"
                )
        );

        atributos.add(
                new Attribute(
                        "ejercicios_completados"
                )
        );

        atributos.add(
                new Attribute(
                        "promedio_peso_usado"
                )
        );

        atributos.add(
                new Attribute(
                        "cantidad_evaluaciones"
                )
        );

        atributos.add(
                new Attribute("peso_inicial")
        );

        atributos.add(
                new Attribute("peso_final")
        );

        atributos.add(
                new Attribute("variacion_peso")
        );

        atributos.add(
                new Attribute("imc_inicial")
        );

        atributos.add(
                new Attribute("imc_final")
        );

        atributos.add(
                new Attribute("variacion_imc")
        );

        ArrayList<String> clases =
                new ArrayList<>(
                        List.of(
                                "ALTA",
                                "BAJA",
                                "MEDIA"
                        )
                );

        atributos.add(
                new Attribute(
                        "nivel_adherencia",
                        clases
                )
        );

        Instances estructura =
                new Instances(
                        "ironfit_adherencia",
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