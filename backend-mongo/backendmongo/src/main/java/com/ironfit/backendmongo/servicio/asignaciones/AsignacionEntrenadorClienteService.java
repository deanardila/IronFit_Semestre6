package com.ironfit.backendmongo.servicio.asignaciones;

import com.ironfit.backendmongo.dto.asignaciones.AsignacionEntrenadorClienteRequest;
import com.ironfit.backendmongo.dto.asignaciones.AsignacionEntrenadorClienteResponse;
import com.ironfit.backendmongo.dto.comun.PaginaResponse;
import com.ironfit.backendmongo.modelo.asignaciones.AsignacionEntrenadorCliente;
import com.ironfit.backendmongo.modelo.seguridad.RoleName;
import com.ironfit.backendmongo.modelo.seguridad.UserDocument;
import com.ironfit.backendmongo.repositorio.asignaciones.AsignacionEntrenadorClienteRepository;
import com.ironfit.backendmongo.repositorio.seguridad.UserRepository;
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
public class AsignacionEntrenadorClienteService {

    private final AsignacionEntrenadorClienteRepository asignacionRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public AsignacionEntrenadorClienteService(
            AsignacionEntrenadorClienteRepository asignacionRepository,
            UserRepository userRepository,
            MongoTemplate mongoTemplate
    ) {
        this.asignacionRepository = asignacionRepository;
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public List<AsignacionEntrenadorClienteResponse> listarAsignaciones() {
        return asignacionRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public List<AsignacionEntrenadorClienteResponse> listarMisClientes(Authentication authentication) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        validarRol(usuarioActual, RoleName.ENTRENADOR, "Solo el entrenador puede consultar sus clientes asignados");

        return asignacionRepository.findByEntrenadorIdAndActivoTrue(usuarioActual.getId())
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public PaginaResponse<AsignacionEntrenadorClienteResponse> listarMisClientesPaginado(
            Authentication authentication,
            int page,
            int size,
            String buscar
    ) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        validarRol(usuarioActual, RoleName.ENTRENADOR, "Solo el entrenador puede consultar sus clientes asignados");

        int paginaActual = Math.max(page, 0);
        int tamanoPagina = size <= 0 ? 20 : Math.min(size, 100);

        Pageable pageable = PageRequest.of(
                paginaActual,
                tamanoPagina,
                Sort.by(Sort.Direction.DESC, "fechaAsignacion")
        );

        Query query = new Query();
        ArrayList<Criteria> criterios = new ArrayList<>();

        criterios.add(Criteria.where("entrenadorId").is(usuarioActual.getId()));
        criterios.add(Criteria.where("activo").is(true));

        if (buscar != null && !buscar.trim().isEmpty()) {
            List<String> clientesCoincidentes = buscarIdsClientesPorTexto(buscar);

            criterios.add(
                    Criteria.where("clienteId").in(
                            clientesCoincidentes.isEmpty()
                                    ? List.of("__sin_resultados__")
                                    : clientesCoincidentes
                    )
            );
        }

        query.addCriteria(new Criteria().andOperator(criterios.toArray(new Criteria[0])));

        long totalElementos = mongoTemplate.count(query, AsignacionEntrenadorCliente.class);

        query.with(pageable);

        List<AsignacionEntrenadorClienteResponse> contenido = mongoTemplate
                .find(query, AsignacionEntrenadorCliente.class)
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

    public AsignacionEntrenadorClienteResponse crearAsignacion(AsignacionEntrenadorClienteRequest request) {
        validarRequest(request);

        UserDocument cliente = userRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        UserDocument entrenador = userRepository.findById(request.getEntrenadorId())
                .orElseThrow(() -> new RuntimeException("Entrenador no encontrado"));

        validarUsuarioRolActivo(cliente, RoleName.CLIENTE, "El usuario seleccionado no es un cliente válido");
        validarUsuarioRolActivo(entrenador, RoleName.ENTRENADOR, "El usuario seleccionado no es un entrenador válido");

        boolean yaExiste = asignacionRepository.existsByClienteIdAndEntrenadorIdAndActivoTrue(
                cliente.getId(),
                entrenador.getId()
        );

        if (yaExiste) {
            throw new RuntimeException("Este cliente ya está asignado a este entrenador");
        }

        LocalDateTime ahora = LocalDateTime.now();

        AsignacionEntrenadorCliente asignacion = new AsignacionEntrenadorCliente();
        asignacion.setClienteId(cliente.getId());
        asignacion.setEntrenadorId(entrenador.getId());
        asignacion.setActivo(true);
        asignacion.setFechaAsignacion(ahora);
        asignacion.setFechaActualizacion(ahora);

        AsignacionEntrenadorCliente guardada = asignacionRepository.save(asignacion);

        return convertirAResponse(guardada);
    }

    public AsignacionEntrenadorClienteResponse cambiarEstado(String id, Boolean activo) {
        if (activo == null) {
            throw new RuntimeException("El estado es obligatorio");
        }

        AsignacionEntrenadorCliente asignacion = asignacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asignación no encontrada"));

        asignacion.setActivo(activo);
        asignacion.setFechaActualizacion(LocalDateTime.now());

        AsignacionEntrenadorCliente actualizada = asignacionRepository.save(asignacion);

        return convertirAResponse(actualizada);
    }

    public boolean clienteAsignadoAEntrenador(String clienteId, String entrenadorId) {
        return asignacionRepository.existsByClienteIdAndEntrenadorIdAndActivoTrue(clienteId, entrenadorId);
    }

    private List<String> buscarIdsClientesPorTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return List.of();
        }

        String busqueda = texto.trim();

        Query query = new Query();

        query.addCriteria(new Criteria().andOperator(
                Criteria.where("roles").in(RoleName.CLIENTE),
                Criteria.where("activo").is(true),
                new Criteria().orOperator(
                        Criteria.where("nombres").regex(busqueda, "i"),
                        Criteria.where("apellidos").regex(busqueda, "i"),
                        Criteria.where("correo").regex(busqueda, "i"),
                        Criteria.where("numDoc").regex(busqueda, "i")
                )
        ));

        query.limit(300);

        return mongoTemplate.find(query, UserDocument.class)
                .stream()
                .map(UserDocument::getId)
                .toList();
    }

    private void validarRequest(AsignacionEntrenadorClienteRequest request) {
        if (request == null) {
            throw new RuntimeException("Los datos de la asignación son obligatorios");
        }

        if (esTextoVacio(request.getClienteId())) {
            throw new RuntimeException("El cliente es obligatorio");
        }

        if (esTextoVacio(request.getEntrenadorId())) {
            throw new RuntimeException("El entrenador es obligatorio");
        }
    }

    private void validarUsuarioRolActivo(UserDocument usuario, String rol, String mensajeRol) {
        if (usuario.getRoles() == null || !usuario.getRoles().contains(rol)) {
            throw new RuntimeException(mensajeRol);
        }

        if (!usuario.isActivo()) {
            throw new RuntimeException("El usuario seleccionado está inactivo");
        }
    }

    private void validarRol(UserDocument usuario, String rol, String mensaje) {
        if (usuario.getRoles() == null || !usuario.getRoles().contains(rol)) {
            throw new RuntimeException(mensaje);
        }
    }

    private UserDocument obtenerUsuarioAutenticado(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("No se pudo identificar el usuario autenticado");
        }

        return userRepository.findByCorreo(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
    }

    private AsignacionEntrenadorClienteResponse convertirAResponse(AsignacionEntrenadorCliente asignacion) {
        AsignacionEntrenadorClienteResponse response = new AsignacionEntrenadorClienteResponse();

        response.setId(asignacion.getId());
        response.setClienteId(asignacion.getClienteId());
        response.setEntrenadorId(asignacion.getEntrenadorId());
        response.setActivo(asignacion.getActivo());
        response.setFechaAsignacion(asignacion.getFechaAsignacion());
        response.setFechaActualizacion(asignacion.getFechaActualizacion());

        userRepository.findById(asignacion.getClienteId()).ifPresent(cliente -> {
            response.setClienteNombre(construirNombreCompleto(cliente));
            response.setClienteCorreo(cliente.getCorreo());
            response.setClienteDocumento(cliente.getNumDoc());
        });

        userRepository.findById(asignacion.getEntrenadorId()).ifPresent(entrenador -> {
            response.setEntrenadorNombre(construirNombreCompleto(entrenador));
            response.setEntrenadorCorreo(entrenador.getCorreo());
        });

        return response;
    }

    private String construirNombreCompleto(UserDocument usuario) {
        String nombres = usuario.getNombres() != null ? usuario.getNombres().trim() : "";
        String apellidos = usuario.getApellidos() != null ? usuario.getApellidos().trim() : "";
        return (nombres + " " + apellidos).trim();
    }

    private boolean esTextoVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }
}