package com.ironfit.backendmongo.servicio.planes;

import com.ironfit.backendmongo.dto.comun.PaginaResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import com.ironfit.backendmongo.dto.planes.PlanEntrenamientoActualizarRequest;
import com.ironfit.backendmongo.dto.planes.PlanEntrenamientoCrearRequest;
import com.ironfit.backendmongo.dto.planes.PlanEntrenamientoResponse;
import com.ironfit.backendmongo.modelo.planes.PlanEntrenamiento;
import com.ironfit.backendmongo.modelo.seguridad.RoleName;
import com.ironfit.backendmongo.modelo.seguridad.UserDocument;
import com.ironfit.backendmongo.repositorio.planes.PlanEntrenamientoRepository;
import com.ironfit.backendmongo.repositorio.seguridad.UserRepository;
import com.ironfit.backendmongo.servicio.asignaciones.AsignacionEntrenadorClienteService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PlanEntrenamientoService {

    private final PlanEntrenamientoRepository planEntrenamientoRepository;
    private final UserRepository userRepository;
    private final AsignacionEntrenadorClienteService asignacionService;
    private final MongoTemplate mongoTemplate;

    public PlanEntrenamientoService(
            PlanEntrenamientoRepository planEntrenamientoRepository,
            UserRepository userRepository,
            AsignacionEntrenadorClienteService asignacionService,
            MongoTemplate mongoTemplate
    ) {
        this.planEntrenamientoRepository = planEntrenamientoRepository;
        this.userRepository = userRepository;
        this.asignacionService = asignacionService;
        this.mongoTemplate = mongoTemplate;
    }

    public List<PlanEntrenamientoResponse> listarPlanes(Authentication authentication) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        List<PlanEntrenamiento> planes;

        if (tieneRol(usuarioActual, RoleName.ADMIN)) {

            planes = planEntrenamientoRepository.findAllByOrderByFechaCreacionDesc();

        } else if (tieneRol(usuarioActual, RoleName.ENTRENADOR)) {

            planes = planEntrenamientoRepository
                    .findByEntrenadorIdOrderByFechaCreacionDesc(usuarioActual.getId());

        } else if (tieneRol(usuarioActual, RoleName.CLIENTE)) {

            planes = planEntrenamientoRepository
                    .findByClienteIdOrderByFechaCreacionDesc(usuarioActual.getId());

        } else {
            throw new RuntimeException("No tienes permisos para listar planes");
        }

        return planes.stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public PaginaResponse<PlanEntrenamientoResponse> listarPlanesPaginados(
            Authentication authentication,
            int page,
            int size,
            String buscar,
            Boolean activo
    ) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        int paginaActual = Math.max(page, 0);
        int tamanoPagina = size <= 0 ? 20 : Math.min(size, 100);

        Pageable pageable = PageRequest.of(
                paginaActual,
                tamanoPagina,
                Sort.by(Sort.Direction.DESC, "fechaCreacion")
        );

        Query query = new Query();
        ArrayList<Criteria> criterios = new ArrayList<>();

        if (tieneRol(usuarioActual, RoleName.ADMIN)) {
            // ADMIN puede ver todos los planes.
        } else if (tieneRol(usuarioActual, RoleName.ENTRENADOR)) {
            criterios.add(Criteria.where("entrenadorId").is(usuarioActual.getId()));
        } else if (tieneRol(usuarioActual, RoleName.CLIENTE)) {
            criterios.add(Criteria.where("clienteId").is(usuarioActual.getId()));
        } else {
            throw new RuntimeException("No tienes permisos para listar planes");
        }

        if (activo != null) {
            criterios.add(Criteria.where("activo").is(activo));
        }

        if (buscar != null && !buscar.trim().isEmpty()) {
            String texto = buscar.trim();

            ArrayList<Criteria> criteriosBusqueda = new ArrayList<>();

            criteriosBusqueda.add(Criteria.where("nombre").regex(texto, "i"));
            criteriosBusqueda.add(Criteria.where("descripcion").regex(texto, "i"));
            criteriosBusqueda.add(Criteria.where("objetivo").regex(texto, "i"));

            List<String> idsUsuariosCoincidentes = buscarIdsUsuariosPorTexto(texto);

            if (!idsUsuariosCoincidentes.isEmpty()) {
                criteriosBusqueda.add(Criteria.where("clienteId").in(idsUsuariosCoincidentes));
                criteriosBusqueda.add(Criteria.where("entrenadorId").in(idsUsuariosCoincidentes));
            }

            criterios.add(new Criteria().orOperator(
                    criteriosBusqueda.toArray(new Criteria[0])
            ));
        }

        if (!criterios.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criterios.toArray(new Criteria[0])));
        }

        long totalElementos = mongoTemplate.count(query, PlanEntrenamiento.class);

        query.with(pageable);

        List<PlanEntrenamientoResponse> contenido = mongoTemplate.find(query, PlanEntrenamiento.class)
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

    public PlanEntrenamientoResponse crearPlan(
            Authentication authentication,
            PlanEntrenamientoCrearRequest request
    ) {
        validarFechas(request.getFechaInicio(), request.getFechaFin());

        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        String nombreLimpio = request.getNombre().trim();
        String descripcionLimpia = request.getDescripcion().trim();
        String objetivoLimpio = request.getObjetivo().trim();
        String clienteIdLimpio = request.getClienteId().trim();
        String entrenadorIdFinal = resolverEntrenadorId(usuarioActual, request.getEntrenadorId());

        validarCliente(clienteIdLimpio);
        validarEntrenador(entrenadorIdFinal);
        validarClienteAsignadoSiEntrenador(usuarioActual, clienteIdLimpio);
        validarClienteSinPlanActivo(clienteIdLimpio, null);

        LocalDateTime ahora = LocalDateTime.now();

        PlanEntrenamiento plan = new PlanEntrenamiento();
        plan.setNombre(nombreLimpio);
        plan.setDescripcion(descripcionLimpia);
        plan.setObjetivo(objetivoLimpio);
        plan.setFechaInicio(request.getFechaInicio());
        plan.setFechaFin(request.getFechaFin());
        plan.setClienteId(clienteIdLimpio);
        plan.setEntrenadorId(entrenadorIdFinal);
        plan.setActivo(true);
        plan.setFechaCreacion(ahora);
        plan.setFechaActualizacion(ahora);

        PlanEntrenamiento guardado = planEntrenamientoRepository.save(plan);
        return convertirAResponse(guardado);
    }

    public PlanEntrenamientoResponse actualizarPlan(
            Authentication authentication,
            String id,
            PlanEntrenamientoActualizarRequest request
    ) {
        validarFechas(request.getFechaInicio(), request.getFechaFin());

        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        PlanEntrenamiento plan = planEntrenamientoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan de entrenamiento no encontrado"));

        validarAccesoAlPlan(usuarioActual, plan);

        String nombreLimpio = request.getNombre().trim();
        String descripcionLimpia = request.getDescripcion().trim();
        String objetivoLimpio = request.getObjetivo().trim();
        String clienteIdLimpio = request.getClienteId().trim();
        String entrenadorIdFinal = resolverEntrenadorId(usuarioActual, request.getEntrenadorId());

        validarCliente(clienteIdLimpio);
        validarEntrenador(entrenadorIdFinal);
        validarClienteAsignadoSiEntrenador(usuarioActual, clienteIdLimpio);

        boolean cambioCliente = !clienteIdLimpio.equals(plan.getClienteId());

        if (cambioCliente && Boolean.TRUE.equals(plan.getActivo())) {
            validarClienteSinPlanActivo(clienteIdLimpio, plan.getId());
        }

        plan.setNombre(nombreLimpio);
        plan.setDescripcion(descripcionLimpia);
        plan.setObjetivo(objetivoLimpio);
        plan.setFechaInicio(request.getFechaInicio());
        plan.setFechaFin(request.getFechaFin());
        plan.setClienteId(clienteIdLimpio);
        plan.setEntrenadorId(entrenadorIdFinal);
        plan.setFechaActualizacion(LocalDateTime.now());

        PlanEntrenamiento actualizado = planEntrenamientoRepository.save(plan);
        return convertirAResponse(actualizado);
    }

    public PlanEntrenamientoResponse cambiarEstado(
        Authentication authentication,
        String id,
        Boolean activo
    ) {
    if (activo == null) {
        throw new RuntimeException("El estado del plan es obligatorio");
    }

    UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

    PlanEntrenamiento plan = planEntrenamientoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Plan de entrenamiento no encontrado"));

    validarAccesoAlPlan(usuarioActual, plan);

    if (Boolean.TRUE.equals(activo)) {
        validarClienteSinPlanActivo(plan.getClienteId(), plan.getId());
    }

    plan.setActivo(activo);
    plan.setFechaActualizacion(LocalDateTime.now());

    PlanEntrenamiento actualizado = planEntrenamientoRepository.save(plan);

    return convertirAResponse(actualizado);
    }


    private void validarClienteSinPlanActivo(String clienteId, String planIdActual) {
        List<PlanEntrenamiento> planesActivos =
                planEntrenamientoRepository.findByClienteIdAndActivoTrue(clienteId);

        boolean tieneOtroPlanActivo = planesActivos.stream()
                .anyMatch(plan -> planIdActual == null || !plan.getId().equals(planIdActual));

        if (tieneOtroPlanActivo) {
            throw new RuntimeException("Este cliente ya tiene un plan activo asignado");
        }
    }

    private UserDocument obtenerUsuarioAutenticado(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("No se pudo identificar el usuario autenticado");
        }

        String correo = authentication.getName();

        return userRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
    }

    private boolean tieneRol(UserDocument usuario, String rol) {
        return usuario.getRoles() != null && usuario.getRoles().contains(rol);
    }

    private String resolverEntrenadorId(UserDocument usuarioActual, String entrenadorIdRequest) {
        if (tieneRol(usuarioActual, RoleName.ENTRENADOR)) {
            return usuarioActual.getId();
        }

        if (tieneRol(usuarioActual, RoleName.ADMIN)) {
            if (entrenadorIdRequest == null || entrenadorIdRequest.trim().isEmpty()) {
                throw new RuntimeException("El entrenadorId es obligatorio para el administrador");
            }
            return entrenadorIdRequest.trim();
        }

        throw new RuntimeException("No tienes permisos para asignar entrenador");
    }

    private void validarCliente(String clienteId) {
        UserDocument cliente = userRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (cliente.getRoles() == null || !cliente.getRoles().contains(RoleName.CLIENTE)) {
            throw new RuntimeException("El usuario seleccionado no es un cliente válido");
        }

        if (!cliente.isActivo()) {
            throw new RuntimeException("El cliente seleccionado está inactivo");
        }
    }

    private void validarEntrenador(String entrenadorId) {
        UserDocument entrenador = userRepository.findById(entrenadorId)
                .orElseThrow(() -> new RuntimeException("Entrenador no encontrado"));

        if (entrenador.getRoles() == null || !entrenador.getRoles().contains(RoleName.ENTRENADOR)) {
            throw new RuntimeException("El usuario seleccionado no es un entrenador válido");
        }

        if (!entrenador.isActivo()) {
            throw new RuntimeException("El entrenador seleccionado está inactivo");
        }
    }

    private void validarAccesoAlPlan(UserDocument usuarioActual, PlanEntrenamiento plan) {
        if (tieneRol(usuarioActual, RoleName.ADMIN)) {
            return;
        }

        if (tieneRol(usuarioActual, RoleName.ENTRENADOR)
                && usuarioActual.getId().equals(plan.getEntrenadorId())) {
            return;
        }

        throw new RuntimeException("No tienes permisos para modificar este plan");
    }

    private void validarFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            throw new RuntimeException("Las fechas de inicio y fin son obligatorias");
        }

        if (fechaFin.isBefore(fechaInicio)) {
            throw new RuntimeException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
    }

    private void validarClienteAsignadoSiEntrenador(UserDocument usuarioActual, String clienteId) {
        if (!tieneRol(usuarioActual, RoleName.ENTRENADOR)) {
            return;
        }

        boolean asignado = asignacionService.clienteAsignadoAEntrenador(
                clienteId,
                usuarioActual.getId()
        );

        if (!asignado) {
            throw new RuntimeException("No puedes crear planes para un cliente que no está asignado a ti");
        }
    }

    private PlanEntrenamientoResponse convertirAResponse(PlanEntrenamiento plan) {
        PlanEntrenamientoResponse response = new PlanEntrenamientoResponse();
        response.setId(plan.getId());
        response.setNombre(plan.getNombre());
        response.setDescripcion(plan.getDescripcion());
        response.setObjetivo(plan.getObjetivo());
        response.setFechaInicio(plan.getFechaInicio());
        response.setFechaFin(plan.getFechaFin());
        response.setClienteId(plan.getClienteId());
        response.setEntrenadorId(plan.getEntrenadorId());
        response.setActivo(plan.getActivo());
        response.setFechaCreacion(plan.getFechaCreacion());
        response.setFechaActualizacion(plan.getFechaActualizacion());

        userRepository.findById(plan.getClienteId()).ifPresent(cliente ->
                response.setClienteNombre(construirNombreCompleto(cliente)));

        userRepository.findById(plan.getEntrenadorId()).ifPresent(entrenador ->
                response.setEntrenadorNombre(construirNombreCompleto(entrenador)));

        return response;
    }

    private String construirNombreCompleto(UserDocument usuario) {
        String nombres = usuario.getNombres() != null ? usuario.getNombres().trim() : "";
        String apellidos = usuario.getApellidos() != null ? usuario.getApellidos().trim() : "";
        return (nombres + " " + apellidos).trim();
    }

    public PlanEntrenamientoResponse obtenerPlan(Authentication authentication, String id) {
    UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

    PlanEntrenamiento plan = planEntrenamientoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Plan de entrenamiento no encontrado"));

    validarAccesoLecturaPlan(usuarioActual, plan);

    return convertirAResponse(plan);
    }  

    private List<String> buscarIdsUsuariosPorTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return List.of();
        }

        String busqueda = texto.trim();

        Query queryUsuarios = new Query();

        queryUsuarios.addCriteria(new Criteria().orOperator(
                Criteria.where("nombres").regex(busqueda, "i"),
                Criteria.where("apellidos").regex(busqueda, "i"),
                Criteria.where("correo").regex(busqueda, "i"),
                Criteria.where("numDoc").regex(busqueda, "i")
        ));

        queryUsuarios.limit(200);

        return mongoTemplate.find(queryUsuarios, UserDocument.class)
                .stream()
                .map(UserDocument::getId)
                .toList();
    }
    
    private void validarAccesoLecturaPlan(UserDocument usuarioActual, PlanEntrenamiento plan) {
    if (tieneRol(usuarioActual, RoleName.ADMIN)) {
        return;
    }

    if (tieneRol(usuarioActual, RoleName.ENTRENADOR)
            && usuarioActual.getId().equals(plan.getEntrenadorId())) {
        return;
    }

    if (tieneRol(usuarioActual, RoleName.CLIENTE)
            && usuarioActual.getId().equals(plan.getClienteId())) {
        return;
    }

    throw new RuntimeException("No tienes permisos para ver este plan");
}

}