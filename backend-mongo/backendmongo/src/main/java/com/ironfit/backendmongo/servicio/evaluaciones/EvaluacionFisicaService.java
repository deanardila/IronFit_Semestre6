package com.ironfit.backendmongo.servicio.evaluaciones;

import com.ironfit.backendmongo.dto.comun.PaginaResponse;
import com.ironfit.backendmongo.dto.evaluaciones.EvaluacionFisicaRequest;
import com.ironfit.backendmongo.dto.evaluaciones.EvaluacionFisicaResponse;
import com.ironfit.backendmongo.modelo.asignaciones.AsignacionEntrenadorCliente;
import com.ironfit.backendmongo.modelo.evaluaciones.EvaluacionFisica;
import com.ironfit.backendmongo.modelo.evaluaciones.MedidasCorporales;
import com.ironfit.backendmongo.modelo.seguridad.RoleName;
import com.ironfit.backendmongo.modelo.seguridad.UserDocument;
import com.ironfit.backendmongo.repositorio.asignaciones.AsignacionEntrenadorClienteRepository;
import com.ironfit.backendmongo.repositorio.evaluaciones.EvaluacionFisicaRepository;
import com.ironfit.backendmongo.repositorio.seguridad.UserRepository;
import com.ironfit.backendmongo.servicio.asignaciones.AsignacionEntrenadorClienteService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EvaluacionFisicaService {

    private final EvaluacionFisicaRepository evaluacionFisicaRepository;
    private final UserRepository userRepository;
    private final AsignacionEntrenadorClienteService asignacionService;
    private final AsignacionEntrenadorClienteRepository asignacionRepository;
    private final MongoTemplate mongoTemplate;

    public EvaluacionFisicaService(
            EvaluacionFisicaRepository evaluacionFisicaRepository,
            UserRepository userRepository,
            AsignacionEntrenadorClienteService asignacionService,
            AsignacionEntrenadorClienteRepository asignacionRepository,
            MongoTemplate mongoTemplate
    ) {
        this.evaluacionFisicaRepository = evaluacionFisicaRepository;
        this.userRepository = userRepository;
        this.asignacionService = asignacionService;
        this.asignacionRepository = asignacionRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public List<EvaluacionFisicaResponse> listarEvaluaciones(Authentication authentication) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        List<EvaluacionFisica> evaluaciones;

        if (tieneRol(usuarioActual, RoleName.ADMIN)) {

            evaluaciones = evaluacionFisicaRepository.findAllByOrderByFechaDesc();

        } else if (tieneRol(usuarioActual, RoleName.ENTRENADOR)) {

            List<String> clienteIds = asignacionRepository
                    .findByEntrenadorIdAndActivoTrue(usuarioActual.getId())
                    .stream()
                    .map(AsignacionEntrenadorCliente::getClienteId)
                    .toList();

            if (clienteIds.isEmpty()) {
                evaluaciones = List.of();
            } else {
                evaluaciones = evaluacionFisicaRepository.findByClienteIdInOrderByFechaDesc(clienteIds);
            }

        } else if (tieneRol(usuarioActual, RoleName.CLIENTE)) {

            evaluaciones = evaluacionFisicaRepository.findByClienteIdOrderByFechaDesc(usuarioActual.getId());

        } else {
            throw new RuntimeException("No tienes permisos para listar evaluaciones físicas");
        }

        return evaluaciones.stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public PaginaResponse<EvaluacionFisicaResponse> listarEvaluacionesPaginadas(
            Authentication authentication,
            int page,
            int size,
            String buscar
    ) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        int paginaActual = Math.max(page, 0);
        int tamanoPagina = size <= 0 ? 20 : Math.min(size, 100);

        Pageable pageable = PageRequest.of(
                paginaActual,
                tamanoPagina,
                Sort.by(Sort.Direction.DESC, "fecha")
        );

        Query query = new Query();
        ArrayList<Criteria> criterios = new ArrayList<>();

        if (tieneRol(usuarioActual, RoleName.ADMIN)) {
            // ADMIN ve todas las evaluaciones.
        } else if (tieneRol(usuarioActual, RoleName.ENTRENADOR)) {
            List<String> clienteIds = asignacionRepository
                    .findByEntrenadorIdAndActivoTrue(usuarioActual.getId())
                    .stream()
                    .map(AsignacionEntrenadorCliente::getClienteId)
                    .toList();

            criterios.add(
                    Criteria.where("clienteId").in(
                            clienteIds.isEmpty()
                                    ? List.of("__sin_resultados__")
                                    : clienteIds
                    )
            );
        } else if (tieneRol(usuarioActual, RoleName.CLIENTE)) {
            criterios.add(Criteria.where("clienteId").is(usuarioActual.getId()));
        } else {
            throw new RuntimeException("No tienes permisos para listar evaluaciones físicas");
        }

        if (buscar != null && !buscar.trim().isEmpty()) {
            String texto = buscar.trim();

            ArrayList<Criteria> criteriosBusqueda = new ArrayList<>();

            criteriosBusqueda.add(Criteria.where("observaciones").regex(texto, "i"));

            List<String> idsUsuarios = buscarIdsUsuariosPorTexto(texto);

            if (!idsUsuarios.isEmpty()) {
                criteriosBusqueda.add(Criteria.where("clienteId").in(idsUsuarios));
                criteriosBusqueda.add(Criteria.where("entrenadorId").in(idsUsuarios));
            }

            criterios.add(new Criteria().orOperator(
                    criteriosBusqueda.toArray(new Criteria[0])
            ));
        }

        if (!criterios.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criterios.toArray(new Criteria[0])));
        }

        long totalElementos = mongoTemplate.count(query, EvaluacionFisica.class);

        query.with(pageable);

        List<EvaluacionFisicaResponse> contenido = mongoTemplate
                .find(query, EvaluacionFisica.class)
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

    public List<EvaluacionFisicaResponse> listarMisEvaluaciones(Authentication authentication) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        if (!tieneRol(usuarioActual, RoleName.CLIENTE)) {
            throw new RuntimeException("Solo el cliente puede consultar sus evaluaciones físicas");
        }

        return evaluacionFisicaRepository.findByClienteIdOrderByFechaDesc(usuarioActual.getId())
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public EvaluacionFisicaResponse crearEvaluacion(
            Authentication authentication,
            EvaluacionFisicaRequest request
    ) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        if (!tieneRol(usuarioActual, RoleName.ENTRENADOR)) {
            throw new RuntimeException("Solo el entrenador puede crear evaluaciones físicas");
        }

        validarRequest(request);
        validarCliente(request.getClienteId());
        validarClienteAsignado(usuarioActual.getId(), request.getClienteId());

        LocalDateTime ahora = LocalDateTime.now();

        EvaluacionFisica evaluacion = new EvaluacionFisica();
        evaluacion.setClienteId(request.getClienteId().trim());
        evaluacion.setEntrenadorId(usuarioActual.getId());
        evaluacion.setFecha(request.getFecha() != null ? request.getFecha() : ahora);
        evaluacion.setPesoCorporal(request.getPesoCorporal());
        evaluacion.setTalla(request.getTalla());
        evaluacion.setImc(calcularImc(request.getPesoCorporal(), request.getTalla()));
        evaluacion.setPorcentajeGraso(request.getPorcentajeGraso());
        evaluacion.setMedidasCorporales(construirMedidasCorporales(request));
        evaluacion.setObservaciones(limpiarTextoOpcional(request.getObservaciones()));
        evaluacion.setFechaCreacion(ahora);
        evaluacion.setFechaActualizacion(ahora);

        EvaluacionFisica guardada = evaluacionFisicaRepository.save(evaluacion);

        return convertirAResponse(guardada);
    }

    public EvaluacionFisicaResponse actualizarEvaluacion(
            Authentication authentication,
            String id,
            EvaluacionFisicaRequest request
    ) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        if (!tieneRol(usuarioActual, RoleName.ENTRENADOR)) {
            throw new RuntimeException("Solo el entrenador puede actualizar evaluaciones físicas");
        }

        validarRequest(request);

        EvaluacionFisica evaluacion = evaluacionFisicaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evaluación física no encontrada"));

        validarEntrenadorCreador(usuarioActual, evaluacion);
        validarCliente(request.getClienteId());
        validarClienteAsignado(usuarioActual.getId(), request.getClienteId());

        evaluacion.setClienteId(request.getClienteId().trim());
        evaluacion.setFecha(request.getFecha() != null ? request.getFecha() : evaluacion.getFecha());
        evaluacion.setPesoCorporal(request.getPesoCorporal());
        evaluacion.setTalla(request.getTalla());
        evaluacion.setImc(calcularImc(request.getPesoCorporal(), request.getTalla()));
        evaluacion.setPorcentajeGraso(request.getPorcentajeGraso());
        evaluacion.setMedidasCorporales(construirMedidasCorporales(request));
        evaluacion.setObservaciones(limpiarTextoOpcional(request.getObservaciones()));
        evaluacion.setFechaActualizacion(LocalDateTime.now());

        EvaluacionFisica actualizada = evaluacionFisicaRepository.save(evaluacion);

        return convertirAResponse(actualizada);
    }

    public void eliminarEvaluacion(Authentication authentication, String id) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        if (!tieneRol(usuarioActual, RoleName.ENTRENADOR)) {
            throw new RuntimeException("Solo el entrenador puede eliminar evaluaciones físicas");
        }

        EvaluacionFisica evaluacion = evaluacionFisicaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evaluación física no encontrada"));

        validarEntrenadorCreador(usuarioActual, evaluacion);

        evaluacionFisicaRepository.delete(evaluacion);
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

    private void validarRequest(EvaluacionFisicaRequest request) {
        if (request == null) {
            throw new RuntimeException("Los datos de la evaluación física son obligatorios");
        }

        if (esTextoVacio(request.getClienteId())) {
            throw new RuntimeException("El cliente es obligatorio");
        }

        if (request.getPesoCorporal() == null || request.getPesoCorporal() <= 0) {
            throw new RuntimeException("El peso corporal debe ser mayor a 0");
        }

        if (request.getTalla() == null || request.getTalla() <= 0) {
            throw new RuntimeException("La talla debe ser mayor a 0");
        }

        if (request.getPorcentajeGraso() != null && request.getPorcentajeGraso() < 0) {
            throw new RuntimeException("El porcentaje graso no puede ser negativo");
        }

        validarMedidaNoNegativa(request.getPecho(), "La medida del pecho no puede ser negativa");
        validarMedidaNoNegativa(request.getCintura(), "La medida de cintura no puede ser negativa");
        validarMedidaNoNegativa(request.getBrazo(), "La medida del brazo no puede ser negativa");
        validarMedidaNoNegativa(request.getPierna(), "La medida de pierna no puede ser negativa");
    }

    private void validarMedidaNoNegativa(Double valor, String mensaje) {
        if (valor != null && valor < 0) {
            throw new RuntimeException(mensaje);
        }
    }

    private void validarCliente(String clienteId) {
        UserDocument cliente = userRepository.findById(clienteId.trim())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (cliente.getRoles() == null || !cliente.getRoles().contains(RoleName.CLIENTE)) {
            throw new RuntimeException("El usuario seleccionado no es un cliente válido");
        }

        if (!cliente.isActivo()) {
            throw new RuntimeException("El cliente seleccionado está inactivo");
        }
    }

    private void validarClienteAsignado(String entrenadorId, String clienteId) {
        boolean asignado = asignacionService.clienteAsignadoAEntrenador(
                clienteId.trim(),
                entrenadorId
        );

        if (!asignado) {
            throw new RuntimeException("No puedes evaluar a un cliente que no está asignado a ti");
        }
    }

    private void validarEntrenadorCreador(UserDocument usuarioActual, EvaluacionFisica evaluacion) {
        if (!usuarioActual.getId().equals(evaluacion.getEntrenadorId())) {
            throw new RuntimeException("No tienes permisos para modificar esta evaluación física");
        }
    }

    private Double calcularImc(Double pesoCorporal, Double talla) {
        double imc = pesoCorporal / (talla * talla);
        return Math.round(imc * 100.0) / 100.0;
    }

    private MedidasCorporales construirMedidasCorporales(EvaluacionFisicaRequest request) {
        MedidasCorporales medidas = new MedidasCorporales();
        medidas.setPecho(request.getPecho());
        medidas.setCintura(request.getCintura());
        medidas.setBrazo(request.getBrazo());
        medidas.setPierna(request.getPierna());
        return medidas;
    }

    private EvaluacionFisicaResponse convertirAResponse(EvaluacionFisica evaluacion) {
        EvaluacionFisicaResponse response = new EvaluacionFisicaResponse();

        response.setId(evaluacion.getId());
        response.setClienteId(evaluacion.getClienteId());
        response.setEntrenadorId(evaluacion.getEntrenadorId());
        response.setFecha(evaluacion.getFecha());
        response.setPesoCorporal(evaluacion.getPesoCorporal());
        response.setTalla(evaluacion.getTalla());
        response.setImc(evaluacion.getImc());
        response.setPorcentajeGraso(evaluacion.getPorcentajeGraso());
        response.setMedidasCorporales(evaluacion.getMedidasCorporales());
        response.setObservaciones(evaluacion.getObservaciones());
        response.setFechaCreacion(evaluacion.getFechaCreacion());
        response.setFechaActualizacion(evaluacion.getFechaActualizacion());

        userRepository.findById(evaluacion.getClienteId()).ifPresent(cliente ->
                response.setClienteNombre(construirNombreCompleto(cliente))
        );

        userRepository.findById(evaluacion.getEntrenadorId()).ifPresent(entrenador ->
                response.setEntrenadorNombre(construirNombreCompleto(entrenador))
        );

        return response;
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

    private String limpiarTextoOpcional(String texto) {
        return texto != null ? texto.trim() : null;
    }

    private boolean esTextoVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }
}