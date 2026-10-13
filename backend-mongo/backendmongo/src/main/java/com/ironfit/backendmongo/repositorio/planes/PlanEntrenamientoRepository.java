package com.ironfit.backendmongo.repositorio.planes;

import com.ironfit.backendmongo.modelo.planes.PlanEntrenamiento;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PlanEntrenamientoRepository extends MongoRepository<PlanEntrenamiento, String> {

    List<PlanEntrenamiento> findAllByOrderByFechaCreacionDesc();

    List<PlanEntrenamiento> findByEntrenadorIdOrderByFechaCreacionDesc(String entrenadorId);

    List<PlanEntrenamiento> findByClienteIdOrderByFechaCreacionDesc(String clienteId);

    List<PlanEntrenamiento> findByClienteIdAndActivoTrue(String clienteId);

    boolean existsByNombreIgnoreCaseAndClienteIdAndEntrenadorIdAndActivoTrue(
            String nombre,
            String clienteId,
            String entrenadorId
    );
}