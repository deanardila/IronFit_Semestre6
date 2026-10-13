package com.ironfit.backendmongo.repositorio.evaluaciones;

import com.ironfit.backendmongo.modelo.evaluaciones.EvaluacionFisica;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EvaluacionFisicaRepository extends MongoRepository<EvaluacionFisica, String> {

    List<EvaluacionFisica> findAllByOrderByFechaDesc();

    List<EvaluacionFisica> findByClienteIdOrderByFechaDesc(String clienteId);

    List<EvaluacionFisica> findByEntrenadorIdOrderByFechaDesc(String entrenadorId);

    List<EvaluacionFisica> findByClienteIdInOrderByFechaDesc(List<String> clienteIds);
}