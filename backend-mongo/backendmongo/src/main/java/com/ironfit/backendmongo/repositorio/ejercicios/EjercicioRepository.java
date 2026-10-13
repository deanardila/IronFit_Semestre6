package com.ironfit.backendmongo.repositorio.ejercicios;

import com.ironfit.backendmongo.modelo.ejercicios.Ejercicio;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EjercicioRepository extends MongoRepository<Ejercicio, String> {

    Optional<Ejercicio> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    List<Ejercicio> findByActivoTrue();

    List<Ejercicio> findByCategoriaIgnoreCase(String categoria);

    List<Ejercicio> findByGrupoMuscularIgnoreCase(String grupoMuscular);
}