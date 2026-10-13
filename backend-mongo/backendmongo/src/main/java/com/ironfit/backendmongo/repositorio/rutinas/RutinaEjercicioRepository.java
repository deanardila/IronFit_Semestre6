package com.ironfit.backendmongo.repositorio.rutinas;

import com.ironfit.backendmongo.modelo.rutinas.RutinaEjercicio;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RutinaEjercicioRepository extends MongoRepository<RutinaEjercicio, String> {

    List<RutinaEjercicio> findByRutinaIdOrderByOrdenAsc(String rutinaId);

    boolean existsByRutinaIdAndEjercicioId(String rutinaId, String ejercicioId);
}