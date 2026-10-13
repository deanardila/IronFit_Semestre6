package com.ironfit.backendmongo.repositorio.asistencias;

import com.ironfit.backendmongo.modelo.asistencias.Asistencia;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface AsistenciaRepository extends MongoRepository<Asistencia, String> {

    List<Asistencia> findAllByOrderByFechaAsistenciaDesc();

    List<Asistencia> findByFechaAsistenciaBetweenOrderByFechaAsistenciaDesc(
            LocalDate fechaInicio,
            LocalDate fechaFin
    );

    List<Asistencia> findByClienteIdOrderByFechaAsistenciaDesc(String clienteId);

    List<Asistencia> findByPlanIdOrderByFechaAsistenciaDesc(String planId);
}