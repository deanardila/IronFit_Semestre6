package com.ironfit.backendmongo.servicio.entrenamientos;

import com.ironfit.backendmongo.dto.entrenamientos.EjercicioRealizadoRequest;
import com.ironfit.backendmongo.dto.entrenamientos.EjercicioRealizadoResponse;
import com.ironfit.backendmongo.dto.entrenamientos.EntrenamientoRealizadoCrearRequest;
import com.ironfit.backendmongo.dto.entrenamientos.EntrenamientoRealizadoResponse;
import com.ironfit.backendmongo.modelo.entrenamientos.EjercicioRealizado;
import com.ironfit.backendmongo.modelo.entrenamientos.EntrenamientoRealizado;
import com.ironfit.backendmongo.modelo.planes.PlanEntrenamiento;
import com.ironfit.backendmongo.modelo.rutinas.Rutina;
import com.ironfit.backendmongo.modelo.seguridad.RoleName;
import com.ironfit.backendmongo.modelo.seguridad.UserDocument;
import com.ironfit.backendmongo.repositorio.ejercicios.EjercicioRepository;
import com.ironfit.backendmongo.repositorio.entrenamientos.EntrenamientoRealizadoRepository;
import com.ironfit.backendmongo.repositorio.planes.PlanEntrenamientoRepository;
import com.ironfit.backendmongo.repositorio.rutinas.RutinaRepository;
import com.ironfit.backendmongo.repositorio.seguridad.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EntrenamientoRealizadoService {

    private final EntrenamientoRealizadoRepository entrenamientoRealizadoRepository;
    private final PlanEntrenamientoRepository planEntrenamientoRepository;
    private final RutinaRepository rutinaRepository;
    private final EjercicioRepository ejercicioRepository;
    private final UserRepository userRepository;

    public EntrenamientoRealizadoService(
            EntrenamientoRealizadoRepository entrenamientoRealizadoRepository,
            PlanEntrenamientoRepository planEntrenamientoRepository,
            RutinaRepository rutinaRepository,
            EjercicioRepository ejercicioRepository,
            UserRepository userRepository
    ) {
        this.entrenamientoRealizadoRepository = entrenamientoRealizadoRepository;
        this.planEntrenamientoRepository = planEntrenamientoRepository;
        this.rutinaRepository = rutinaRepository;
        this.ejercicioRepository = ejercicioRepository;
        this.userRepository = userRepository;
    }

    public EntrenamientoRealizadoResponse registrarEntrenamiento(
            Authentication authentication,
            EntrenamientoRealizadoCrearRequest request
    ) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        if (!tieneRol(usuarioActual, RoleName.CLIENTE)) {
            throw new RuntimeException("Solo el cliente puede registrar su entrenamiento realizado");
        }

        PlanEntrenamiento plan = planEntrenamientoRepository.findById(request.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plan de entrenamiento no encontrado"));

        if (!usuarioActual.getId().equals(plan.getClienteId())) {
            throw new RuntimeException("No puedes registrar entrenamientos de otro cliente");
        }

        Rutina rutina = rutinaRepository.findById(request.getRutinaId())
                .orElseThrow(() -> new RuntimeException("Rutina no encontrada"));

        if (!rutina.getPlanId().equals(plan.getId())) {
            throw new RuntimeException("La rutina no pertenece al plan indicado");
        }

        LocalDate fechaHoy = LocalDate.now();

        if (entrenamientoRealizadoRepository.existsByClienteIdAndRutinaIdAndFecha(
                usuarioActual.getId(),
                rutina.getId(),
                fechaHoy
        )) {
            throw new RuntimeException("Ya registraste esta rutina como realizada el día de hoy");
        }

        LocalDateTime ahora = LocalDateTime.now();

        EntrenamientoRealizado entrenamiento = new EntrenamientoRealizado();
        entrenamiento.setClienteId(usuarioActual.getId());
        entrenamiento.setPlanId(plan.getId());
        entrenamiento.setRutinaId(rutina.getId());
        entrenamiento.setRutinaNombre(
                request.getRutinaNombre() != null && !request.getRutinaNombre().trim().isEmpty()
                        ? request.getRutinaNombre().trim()
                        : rutina.getNombre()
        );
        entrenamiento.setFecha(fechaHoy);
        entrenamiento.setEstado(calcularEstado(request.getEjerciciosRealizados()));
        entrenamiento.setEjerciciosRealizados(convertirEjerciciosRequest(request.getEjerciciosRealizados()));
        entrenamiento.setObservacionCliente(
                request.getObservacionCliente() != null
                        ? request.getObservacionCliente().trim()
                        : null
        );
        entrenamiento.setFechaCreacion(ahora);
        entrenamiento.setFechaActualizacion(ahora);

        EntrenamientoRealizado guardado = entrenamientoRealizadoRepository.save(entrenamiento);

        return convertirAResponse(guardado);
    }

    public List<EntrenamientoRealizadoResponse> listarMisEntrenamientos(Authentication authentication) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        if (!tieneRol(usuarioActual, RoleName.CLIENTE)) {
            throw new RuntimeException("Solo el cliente puede consultar sus entrenamientos");
        }

        return entrenamientoRealizadoRepository
                .findByClienteIdOrderByFechaDesc(usuarioActual.getId())
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public List<EntrenamientoRealizadoResponse> listarPorCliente(
            Authentication authentication,
            String clienteId
    ) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        if (tieneRol(usuarioActual, RoleName.ADMIN)) {
            return entrenamientoRealizadoRepository
                    .findByClienteIdOrderByFechaDesc(clienteId)
                    .stream()
                    .map(this::convertirAResponse)
                    .toList();
        }

        if (tieneRol(usuarioActual, RoleName.ENTRENADOR)) {
            validarClientePerteneceAlEntrenador(clienteId, usuarioActual.getId());

            return entrenamientoRealizadoRepository
                    .findByClienteIdOrderByFechaDesc(clienteId)
                    .stream()
                    .map(this::convertirAResponse)
                    .toList();
        }

        throw new RuntimeException("No tienes permisos para consultar entrenamientos de este cliente");
    }

    private void validarClientePerteneceAlEntrenador(String clienteId, String entrenadorId) {
        List<PlanEntrenamiento> planesDelCliente =
                planEntrenamientoRepository.findByClienteIdOrderByFechaCreacionDesc(clienteId);

        boolean pertenece = planesDelCliente.stream()
                .anyMatch(plan -> entrenadorId.equals(plan.getEntrenadorId()));

        if (!pertenece) {
            throw new RuntimeException("Este cliente no pertenece al entrenador autenticado");
        }
    }

    private String calcularEstado(List<EjercicioRealizadoRequest> ejercicios) {
        if (ejercicios == null || ejercicios.isEmpty()) {
            return "PENDIENTE";
        }

        long completados = ejercicios.stream()
                .filter(e -> Boolean.TRUE.equals(e.getCompletado()))
                .count();

        if (completados == ejercicios.size()) {
            return "COMPLETADO";
        }

        if (completados > 0) {
            return "PARCIAL";
        }

        return "PENDIENTE";
    }

    private List<EjercicioRealizado> convertirEjerciciosRequest(List<EjercicioRealizadoRequest> requests) {
        return requests.stream()
                .map(this::convertirEjercicioRequest)
                .toList();
    }

    private EjercicioRealizado convertirEjercicioRequest(EjercicioRealizadoRequest request) {
        EjercicioRealizado ejercicioRealizado = new EjercicioRealizado();

        ejercicioRealizado.setEjercicioId(request.getEjercicioId());

        String nombreEjercicio = obtenerNombreEjercicio(request);
        ejercicioRealizado.setEjercicioNombre(nombreEjercicio);

        ejercicioRealizado.setSeriesRealizadas(request.getSeriesRealizadas());
        ejercicioRealizado.setRepeticionesRealizadas(request.getRepeticionesRealizadas());
        ejercicioRealizado.setPesoUsado(request.getPesoUsado());
        ejercicioRealizado.setCompletado(Boolean.TRUE.equals(request.getCompletado()));

        return ejercicioRealizado;
    }

    private String obtenerNombreEjercicio(EjercicioRealizadoRequest request) {
        if (request.getEjercicioNombre() != null && !request.getEjercicioNombre().trim().isEmpty()) {
            return request.getEjercicioNombre().trim();
        }

        return ejercicioRepository.findById(request.getEjercicioId())
                .map(ejercicio -> ejercicio.getNombre())
                .orElse("Ejercicio no encontrado");
    }

    private EntrenamientoRealizadoResponse convertirAResponse(EntrenamientoRealizado entrenamiento) {
        EntrenamientoRealizadoResponse response = new EntrenamientoRealizadoResponse();

        response.setId(entrenamiento.getId());
        response.setClienteId(entrenamiento.getClienteId());
        response.setClienteNombre(obtenerNombreCliente(entrenamiento.getClienteId()));
        response.setPlanId(entrenamiento.getPlanId());
        response.setRutinaId(entrenamiento.getRutinaId());
        response.setRutinaNombre(entrenamiento.getRutinaNombre());
        response.setFecha(entrenamiento.getFecha());
        response.setEstado(entrenamiento.getEstado());
        response.setObservacionCliente(entrenamiento.getObservacionCliente());
        response.setFechaCreacion(entrenamiento.getFechaCreacion());
        response.setFechaActualizacion(entrenamiento.getFechaActualizacion());

        if (entrenamiento.getEjerciciosRealizados() != null) {
            response.setEjerciciosRealizados(
                    entrenamiento.getEjerciciosRealizados()
                            .stream()
                            .map(this::convertirEjercicioAResponse)
                            .toList()
            );
        }

        return response;
    }

    private EjercicioRealizadoResponse convertirEjercicioAResponse(EjercicioRealizado ejercicio) {
        EjercicioRealizadoResponse response = new EjercicioRealizadoResponse();

        response.setEjercicioId(ejercicio.getEjercicioId());
        response.setEjercicioNombre(ejercicio.getEjercicioNombre());
        response.setSeriesRealizadas(ejercicio.getSeriesRealizadas());
        response.setRepeticionesRealizadas(ejercicio.getRepeticionesRealizadas());
        response.setPesoUsado(ejercicio.getPesoUsado());
        response.setCompletado(ejercicio.getCompletado());

        return response;
    }

    private String obtenerNombreCliente(String clienteId) {
        return userRepository.findById(clienteId)
                .map(this::construirNombreCompleto)
                .orElse("Cliente no encontrado");
    }

    private String construirNombreCompleto(UserDocument usuario) {
        String nombres = usuario.getNombres() != null ? usuario.getNombres().trim() : "";
        String apellidos = usuario.getApellidos() != null ? usuario.getApellidos().trim() : "";
        return (nombres + " " + apellidos).trim();
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
}