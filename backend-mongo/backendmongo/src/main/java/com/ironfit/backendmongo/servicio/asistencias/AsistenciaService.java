package com.ironfit.backendmongo.servicio.asistencias;

import com.ironfit.backendmongo.dto.asistencias.AsistenciaReporteResponse;
import com.ironfit.backendmongo.dto.comun.PaginaResponse;
import com.ironfit.backendmongo.modelo.asistencias.Asistencia;
import com.ironfit.backendmongo.modelo.planes.PlanEntrenamiento;
import com.ironfit.backendmongo.modelo.rutinas.Rutina;
import com.ironfit.backendmongo.modelo.seguridad.RoleName;
import com.ironfit.backendmongo.modelo.seguridad.UserDocument;
import com.ironfit.backendmongo.repositorio.asistencias.AsistenciaRepository;
import com.ironfit.backendmongo.repositorio.planes.PlanEntrenamientoRepository;
import com.ironfit.backendmongo.repositorio.rutinas.RutinaRepository;
import com.ironfit.backendmongo.repositorio.seguridad.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final PlanEntrenamientoRepository planRepository;
    private final RutinaRepository rutinaRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public AsistenciaService(
            AsistenciaRepository asistenciaRepository,
            PlanEntrenamientoRepository planRepository,
            RutinaRepository rutinaRepository,
            UserRepository userRepository,
            MongoTemplate mongoTemplate
    ) {
        this.asistenciaRepository = asistenciaRepository;
        this.planRepository = planRepository;
        this.rutinaRepository = rutinaRepository;
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public List<AsistenciaReporteResponse> generarReporte(
            Authentication authentication,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String clienteId,
            String entrenadorId,
            String estado
    ) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        List<Asistencia> asistencias;

        if (fechaInicio != null && fechaFin != null) {
            if (fechaFin.isBefore(fechaInicio)) {
                throw new RuntimeException("La fecha final no puede ser anterior a la fecha inicial");
            }

            asistencias = asistenciaRepository.findByFechaAsistenciaBetweenOrderByFechaAsistenciaDesc(
                    fechaInicio,
                    fechaFin
            );
        } else {
            asistencias = asistenciaRepository.findAllByOrderByFechaAsistenciaDesc();
        }

        return asistencias.stream()
                .filter(asistencia -> filtrarPorRol(usuarioActual, asistencia))
                .filter(asistencia -> filtrarPorCliente(asistencia, clienteId))
                .filter(asistencia -> filtrarPorEntrenador(asistencia, entrenadorId, usuarioActual))
                .filter(asistencia -> filtrarPorEstado(asistencia, estado))
                .map(this::convertirAResponse)
                .toList();
    }

    public PaginaResponse<AsistenciaReporteResponse> generarReportePaginado(
            Authentication authentication,
            int page,
            int size,
            String buscar,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String clienteId,
            String entrenadorId,
            String estado
    ) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        if (fechaInicio != null && fechaFin != null && fechaFin.isBefore(fechaInicio)) {
            throw new RuntimeException("La fecha final no puede ser anterior a la fecha inicial");
        }

        int paginaActual = Math.max(page, 0);
        int tamanoPagina = size <= 0 ? 20 : Math.min(size, 100);

        Pageable pageable = PageRequest.of(
                paginaActual,
                tamanoPagina,
                Sort.by(Sort.Direction.DESC, "fechaAsistencia")
        );

        Query query = new Query();
        ArrayList<Criteria> criterios = new ArrayList<>();

        if (fechaInicio != null && fechaFin != null) {
            criterios.add(Criteria.where("fechaAsistencia").gte(fechaInicio).lte(fechaFin));
        } else if (fechaInicio != null) {
            criterios.add(Criteria.where("fechaAsistencia").gte(fechaInicio));
        } else if (fechaFin != null) {
            criterios.add(Criteria.where("fechaAsistencia").lte(fechaFin));
        }

        if (clienteId != null && !clienteId.trim().isEmpty()) {
            criterios.add(Criteria.where("clienteId").is(clienteId.trim()));
        }

        if (tieneRol(usuarioActual, RoleName.ENTRENADOR)) {
            List<String> planesDelEntrenador = buscarIdsPlanesPorEntrenador(usuarioActual.getId());
            criterios.add(Criteria.where("planId").in(planesDelEntrenador.isEmpty() ? List.of("__sin_resultados__") : planesDelEntrenador));
        } else if (tieneRol(usuarioActual, RoleName.ADMIN)) {
            if (entrenadorId != null && !entrenadorId.trim().isEmpty()) {
                List<String> planesDelEntrenador = buscarIdsPlanesPorEntrenador(entrenadorId.trim());
                criterios.add(Criteria.where("planId").in(planesDelEntrenador.isEmpty() ? List.of("__sin_resultados__") : planesDelEntrenador));
            }
        } else {
            throw new RuntimeException("No tienes permisos para consultar reportes de asistencia");
        }

        if (estado != null && !estado.trim().isEmpty() && !"TODOS".equalsIgnoreCase(estado)) {
            String estadoNormalizado = normalizarEstadoFiltro(estado);

            if ("ASISTIO".equals(estadoNormalizado)) {
                criterios.add(Criteria.where("asistio").is(true));
                criterios.add(Criteria.where("cumplioRutina").is(true));
            } else if ("NO_ASISTIO".equals(estadoNormalizado)) {
                criterios.add(Criteria.where("asistio").ne(true));
            } else if ("ASISTIO_SIN_RUTINA".equals(estadoNormalizado)) {
                criterios.add(Criteria.where("asistio").is(true));
                criterios.add(Criteria.where("cumplioRutina").ne(true));
            }
        }

        if (buscar != null && !buscar.trim().isEmpty()) {
            String texto = buscar.trim();

            ArrayList<Criteria> criteriosBusqueda = new ArrayList<>();

            criteriosBusqueda.add(Criteria.where("observaciones").regex(texto, "i"));

            List<String> idsUsuarios = buscarIdsUsuariosPorTexto(texto);
            if (!idsUsuarios.isEmpty()) {
                criteriosBusqueda.add(Criteria.where("clienteId").in(idsUsuarios));
            }

            List<String> idsPlanes = buscarIdsPlanesPorTexto(texto);
            if (!idsPlanes.isEmpty()) {
                criteriosBusqueda.add(Criteria.where("planId").in(idsPlanes));
            }

            List<String> idsRutinas = buscarIdsRutinasPorTexto(texto);
            if (!idsRutinas.isEmpty()) {
                criteriosBusqueda.add(Criteria.where("rutinaId").in(idsRutinas));
            }

            criterios.add(new Criteria().orOperator(
                    criteriosBusqueda.toArray(new Criteria[0])
            ));
        }

        if (!criterios.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criterios.toArray(new Criteria[0])));
        }

        long totalElementos = mongoTemplate.count(query, Asistencia.class);

        query.with(pageable);

        List<AsistenciaReporteResponse> contenido = mongoTemplate.find(query, Asistencia.class)
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

    private List<String> buscarIdsPlanesPorEntrenador(String entrenadorId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("entrenadorId").is(entrenadorId));
        query.fields().include("_id");

        return mongoTemplate.find(query, PlanEntrenamiento.class)
                .stream()
                .map(PlanEntrenamiento::getId)
                .toList();
    }

    private List<String> buscarIdsUsuariosPorTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return List.of();
        }

        String busqueda = texto.trim();

        Query query = new Query();
        query.addCriteria(new Criteria().orOperator(
                Criteria.where("nombres").regex(busqueda, "i"),
                Criteria.where("apellidos").regex(busqueda, "i"),
                Criteria.where("correo").regex(busqueda, "i"),
                Criteria.where("numDoc").regex(busqueda, "i")
        ));
        query.limit(300);

        return mongoTemplate.find(query, UserDocument.class)
                .stream()
                .map(UserDocument::getId)
                .toList();
    }

    private List<String> buscarIdsPlanesPorTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return List.of();
        }

        String busqueda = texto.trim();

        Query query = new Query();
        query.addCriteria(new Criteria().orOperator(
                Criteria.where("nombre").regex(busqueda, "i"),
                Criteria.where("descripcion").regex(busqueda, "i"),
                Criteria.where("objetivo").regex(busqueda, "i")
        ));
        query.limit(300);

        return mongoTemplate.find(query, PlanEntrenamiento.class)
                .stream()
                .map(PlanEntrenamiento::getId)
                .toList();
    }

    private List<String> buscarIdsRutinasPorTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return List.of();
        }

        String busqueda = texto.trim();

        Query query = new Query();
        query.addCriteria(new Criteria().orOperator(
                Criteria.where("nombre").regex(busqueda, "i"),
                Criteria.where("descripcion").regex(busqueda, "i"),
                Criteria.where("diaSemana").regex(busqueda, "i")
        ));
        query.limit(300);

        return mongoTemplate.find(query, Rutina.class)
                .stream()
                .map(Rutina::getId)
                .toList();
    }

    private boolean filtrarPorRol(UserDocument usuarioActual, Asistencia asistencia) {
        if (tieneRol(usuarioActual, RoleName.ADMIN)) {
            return true;
        }

        if (tieneRol(usuarioActual, RoleName.ENTRENADOR)) {
            return planRepository.findById(asistencia.getPlanId())
                    .map(plan -> usuarioActual.getId().equals(plan.getEntrenadorId()))
                    .orElse(false);
        }

        throw new RuntimeException("No tienes permisos para consultar reportes de asistencia");
    }

    private boolean filtrarPorCliente(Asistencia asistencia, String clienteId) {
        if (clienteId == null || clienteId.trim().isEmpty()) {
            return true;
        }

        return clienteId.trim().equals(asistencia.getClienteId());
    }

    private boolean filtrarPorEntrenador(
            Asistencia asistencia,
            String entrenadorId,
            UserDocument usuarioActual
    ) {
        if (tieneRol(usuarioActual, RoleName.ENTRENADOR)) {
            return true;
        }

        if (entrenadorId == null || entrenadorId.trim().isEmpty()) {
            return true;
        }

        return planRepository.findById(asistencia.getPlanId())
                .map(plan -> entrenadorId.trim().equals(plan.getEntrenadorId()))
                .orElse(false);
    }

    private boolean filtrarPorEstado(Asistencia asistencia, String estado) {
        if (estado == null || estado.trim().isEmpty() || "TODOS".equalsIgnoreCase(estado)) {
            return true;
        }

        String estadoCalculado = calcularEstadoAsistencia(asistencia)
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace(" ", "_")
                .toUpperCase();

        String estadoFiltro = estado.trim()
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace(" ", "_")
                .toUpperCase();

        return estadoCalculado.equals(estadoFiltro);
    }

    private AsistenciaReporteResponse convertirAResponse(Asistencia asistencia) {
        AsistenciaReporteResponse response = new AsistenciaReporteResponse();

        response.setId(asistencia.getId());
        response.setClienteId(asistencia.getClienteId());
        response.setPlanId(asistencia.getPlanId());
        response.setRutinaId(asistencia.getRutinaId());
        response.setFechaAsistencia(asistencia.getFechaAsistencia());
        response.setAsistio(asistencia.getAsistio());
        response.setCumplioRutina(asistencia.getCumplioRutina());
        response.setEstadoAsistencia(calcularEstadoAsistencia(asistencia));
        response.setObservaciones(asistencia.getObservaciones());

        userRepository.findById(asistencia.getClienteId()).ifPresent(cliente -> {
            response.setClienteNombre(construirNombreCompleto(cliente));
            response.setClienteDocumento(cliente.getNumDoc());
        });

        planRepository.findById(asistencia.getPlanId()).ifPresent(plan -> {
            response.setPlanNombre(plan.getNombre());
            response.setEntrenadorId(plan.getEntrenadorId());

            userRepository.findById(plan.getEntrenadorId()).ifPresent(entrenador ->
                    response.setEntrenadorNombre(construirNombreCompleto(entrenador))
            );
        });

        rutinaRepository.findById(asistencia.getRutinaId()).ifPresent(rutina ->
                response.setRutinaNombre(rutina.getNombre())
        );

        return response;
    }

    private String calcularEstadoAsistencia(Asistencia asistencia) {
        if (!Boolean.TRUE.equals(asistencia.getAsistio())) {
            return "NO ASISTIÓ";
        }

        if (Boolean.TRUE.equals(asistencia.getCumplioRutina())) {
            return "ASISTIÓ";
        }

        return "ASISTIÓ SIN RUTINA";
    }

    private String normalizarEstadoFiltro(String estado) {
        return (estado == null ? "" : estado)
                .trim()
                .toUpperCase()
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace(" ", "_");
    }

    private UserDocument obtenerUsuarioAutenticado(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("No se pudo identificar el usuario autenticado");
        }

        return userRepository.findByCorreo(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
    }

    private boolean tieneRol(UserDocument usuario, String rol) {
        return usuario.getRoles() != null && usuario.getRoles().contains(rol);
    }

    private String construirNombreCompleto(UserDocument usuario) {
        String nombres = usuario.getNombres() != null ? usuario.getNombres().trim() : "";
        String apellidos = usuario.getApellidos() != null ? usuario.getApellidos().trim() : "";
        return (nombres + " " + apellidos).trim();
    }
}