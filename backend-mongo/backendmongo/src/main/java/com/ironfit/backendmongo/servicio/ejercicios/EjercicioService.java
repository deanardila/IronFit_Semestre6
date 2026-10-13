package com.ironfit.backendmongo.servicio.ejercicios;
import com.ironfit.backendmongo.dto.comun.PaginaResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;

import com.ironfit.backendmongo.dto.ejercicios.EjercicioActualizarRequest;
import com.ironfit.backendmongo.dto.ejercicios.EjercicioCrearRequest;
import com.ironfit.backendmongo.dto.ejercicios.EjercicioResponse;
import com.ironfit.backendmongo.modelo.ejercicios.Ejercicio;
import com.ironfit.backendmongo.repositorio.ejercicios.EjercicioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EjercicioService {

    private final EjercicioRepository ejercicioRepository;
    private final MongoTemplate mongoTemplate;

    public EjercicioService(
            EjercicioRepository ejercicioRepository,
            MongoTemplate mongoTemplate
    ) {
        this.ejercicioRepository = ejercicioRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public List<EjercicioResponse> listarEjercicios() {
        return ejercicioRepository.findByActivoTrue()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public PaginaResponse<EjercicioResponse> listarEjerciciosPaginados(
            int page,
            int size,
            String buscar,
            String categoria,
            String grupoMuscular,
            Boolean activo
    ) {
        int paginaActual = Math.max(page, 0);
        int tamanoPagina = size <= 0 ? 20 : Math.min(size, 100);

        Pageable pageable = PageRequest.of(
                paginaActual,
                tamanoPagina,
                Sort.by(Sort.Direction.ASC, "nombre")
        );

        Query query = new Query();
        ArrayList<Criteria> criterios = new ArrayList<>();

        Boolean activoFiltro = activo != null ? activo : true;
        criterios.add(Criteria.where("activo").is(activoFiltro));

        if (buscar != null && !buscar.trim().isEmpty()) {
            String texto = buscar.trim();

            criterios.add(new Criteria().orOperator(
                    Criteria.where("nombre").regex(texto, "i"),
                    Criteria.where("descripcion").regex(texto, "i"),
                    Criteria.where("categoria").regex(texto, "i"),
                    Criteria.where("grupoMuscular").regex(texto, "i"),
                    Criteria.where("tipoEquipo").regex(texto, "i")
            ));
        }

        if (categoria != null && !categoria.trim().isEmpty()) {
            criterios.add(Criteria.where("categoria").regex("^" + categoria.trim() + "$", "i"));
        }

        if (grupoMuscular != null && !grupoMuscular.trim().isEmpty()) {
            criterios.add(Criteria.where("grupoMuscular").regex("^" + grupoMuscular.trim() + "$", "i"));
        }

        if (!criterios.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criterios.toArray(new Criteria[0])));
        }

        long totalElementos = mongoTemplate.count(query, Ejercicio.class);

        query.with(pageable);

        List<EjercicioResponse> contenido = mongoTemplate.find(query, Ejercicio.class)
                .stream()
                .map(this::convertirAResponse)
                .toList();

        int totalPaginas = totalElementos == 0
                ? 0
                : (int) Math.ceil((double) totalElementos / tamanoPagina);

        boolean ultima = totalPaginas == 0 || paginaActual >= totalPaginas - 1;

        return new PaginaResponse<>(
                contenido,
                paginaActual,
                tamanoPagina,
                totalElementos,
                totalPaginas,
                ultima
        );
    }

    public EjercicioResponse crearEjercicio(EjercicioCrearRequest request) {
        if (ejercicioRepository.existsByNombreIgnoreCase(request.getNombre().trim())) {
            throw new RuntimeException("Ya existe un ejercicio con ese nombre");
        }

        LocalDateTime ahora = LocalDateTime.now();

        Ejercicio ejercicio = new Ejercicio();
        ejercicio.setNombre(request.getNombre().trim());
        ejercicio.setDescripcion(request.getDescripcion().trim());
        ejercicio.setCategoria(request.getCategoria().trim());
        ejercicio.setGrupoMuscular(request.getGrupoMuscular().trim());
        ejercicio.setSeriesSugeridas(request.getSeriesSugeridas());
        ejercicio.setRepeticionesSugeridas(request.getRepeticionesSugeridas());
        ejercicio.setTipoEquipo(request.getTipoEquipo() != null ? request.getTipoEquipo().trim() : null);
        ejercicio.setActivo(true);
        ejercicio.setFechaCreacion(ahora);
        ejercicio.setFechaActualizacion(ahora);

        Ejercicio guardado = ejercicioRepository.save(ejercicio);
        return convertirAResponse(guardado);
    }

    public EjercicioResponse actualizarEjercicio(String id, EjercicioActualizarRequest request) {
        Ejercicio ejercicio = ejercicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ejercicio no encontrado"));

        String nombreNuevo = request.getNombre().trim();

        ejercicioRepository.findByNombreIgnoreCase(nombreNuevo).ifPresent(existente -> {
            if (!existente.getId().equals(id)) {
                throw new RuntimeException("Ya existe otro ejercicio con ese nombre");
            }
        });

        ejercicio.setNombre(nombreNuevo);
        ejercicio.setDescripcion(request.getDescripcion().trim());
        ejercicio.setCategoria(request.getCategoria().trim());
        ejercicio.setGrupoMuscular(request.getGrupoMuscular().trim());
        ejercicio.setSeriesSugeridas(request.getSeriesSugeridas());
        ejercicio.setRepeticionesSugeridas(request.getRepeticionesSugeridas());
        ejercicio.setTipoEquipo(request.getTipoEquipo() != null ? request.getTipoEquipo().trim() : null);
        ejercicio.setFechaActualizacion(LocalDateTime.now());

        Ejercicio actualizado = ejercicioRepository.save(ejercicio);
        return convertirAResponse(actualizado);
    }

    public void eliminarEjercicio(String id) {
        Ejercicio ejercicio = ejercicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ejercicio no encontrado"));

        ejercicio.setActivo(false);
        ejercicioRepository.save(ejercicio);
    }

    private EjercicioResponse convertirAResponse(Ejercicio ejercicio) {
        EjercicioResponse response = new EjercicioResponse();
        response.setId(ejercicio.getId());
        response.setNombre(ejercicio.getNombre());
        response.setDescripcion(ejercicio.getDescripcion());
        response.setCategoria(ejercicio.getCategoria());
        response.setGrupoMuscular(ejercicio.getGrupoMuscular());
        response.setSeriesSugeridas(ejercicio.getSeriesSugeridas());
        response.setRepeticionesSugeridas(ejercicio.getRepeticionesSugeridas());
        response.setTipoEquipo(ejercicio.getTipoEquipo());
        return response;
    }

    public void cambiarEstado(String id, Boolean activo) {
    Ejercicio ejercicio = ejercicioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Ejercicio no encontrado"));

    ejercicio.setActivo(activo);
    ejercicio.setFechaActualizacion(LocalDateTime.now());

    ejercicioRepository.save(ejercicio);
}
}