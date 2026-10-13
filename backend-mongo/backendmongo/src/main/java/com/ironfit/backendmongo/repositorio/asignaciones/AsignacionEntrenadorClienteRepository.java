package com.ironfit.backendmongo.repositorio.asignaciones;

import com.ironfit.backendmongo.modelo.asignaciones.AsignacionEntrenadorCliente;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AsignacionEntrenadorClienteRepository extends MongoRepository<AsignacionEntrenadorCliente, String> {

    List<AsignacionEntrenadorCliente> findByEntrenadorIdAndActivoTrue(String entrenadorId);

    List<AsignacionEntrenadorCliente> findByClienteIdAndActivoTrue(String clienteId);

    List<AsignacionEntrenadorCliente> findByActivoTrue();

    boolean existsByClienteIdAndEntrenadorIdAndActivoTrue(String clienteId, String entrenadorId);
}