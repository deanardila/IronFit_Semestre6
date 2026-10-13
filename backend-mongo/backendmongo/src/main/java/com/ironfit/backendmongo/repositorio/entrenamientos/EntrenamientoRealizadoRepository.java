package com.ironfit.backendmongo.repositorio.entrenamientos;

import com.ironfit.backendmongo.modelo.entrenamientos.EntrenamientoRealizado;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EntrenamientoRealizadoRepository extends MongoRepository<EntrenamientoRealizado, String> {

    List<EntrenamientoRealizado> findByClienteIdOrderByFechaDesc(String clienteId);

    List<EntrenamientoRealizado> findByClienteIdAndPlanIdOrderByFechaDesc(String clienteId, String planId);

    List<EntrenamientoRealizado> findByPlanIdOrderByFechaDesc(String planId);

    Optional<EntrenamientoRealizado> findByClienteIdAndRutinaIdAndFecha(
            String clienteId,
            String rutinaId,
            LocalDate fecha
    );

    boolean existsByClienteIdAndRutinaIdAndFecha(
            String clienteId,
            String rutinaId,
            LocalDate fecha
    );
}