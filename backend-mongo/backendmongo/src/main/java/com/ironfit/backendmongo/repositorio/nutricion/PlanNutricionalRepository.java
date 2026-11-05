package com.ironfit.backendmongo.repositorio.nutricion;

import com.ironfit.backendmongo.modelo.nutricion.PlanNutricional;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PlanNutricionalRepository extends MongoRepository<PlanNutricional, String> {

    Optional<PlanNutricional> findByPlanId(String planId);

    List<PlanNutricional> findByEntrenadorIdOrderByFechaActualizacionDesc(String entrenadorId);

    List<PlanNutricional> findByClienteIdOrderByFechaActualizacionDesc(String clienteId);
}
