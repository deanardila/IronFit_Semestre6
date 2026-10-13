package com.ironfit.backendmongo.repositorio.rutinas;

import com.ironfit.backendmongo.modelo.rutinas.Rutina;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RutinaRepository extends MongoRepository<Rutina, String> {

    List<Rutina> findByPlanIdAndActivoTrueOrderByOrdenAsc(String planId);

    List<Rutina> findByPlanIdOrderByOrdenAsc(String planId);
}