package com.ironfit.backendmongo.servicio.rutinas;

import com.ironfit.backendmongo.dto.rutinas.RutinaRequest;
import com.ironfit.backendmongo.dto.rutinas.RutinaResponse;
import com.ironfit.backendmongo.modelo.planes.PlanEntrenamiento;
import com.ironfit.backendmongo.modelo.rutinas.Rutina;
import com.ironfit.backendmongo.modelo.seguridad.RoleName;
import com.ironfit.backendmongo.modelo.seguridad.UserDocument;
import com.ironfit.backendmongo.repositorio.planes.PlanEntrenamientoRepository;
import com.ironfit.backendmongo.repositorio.rutinas.RutinaRepository;
import com.ironfit.backendmongo.repositorio.seguridad.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RutinaService {

    private final RutinaRepository rutinaRepository;
    private final PlanEntrenamientoRepository planEntrenamientoRepository;
    private final UserRepository userRepository;

    public RutinaService(
            RutinaRepository rutinaRepository,
            PlanEntrenamientoRepository planEntrenamientoRepository,
            UserRepository userRepository
    ) {
        this.rutinaRepository = rutinaRepository;
        this.planEntrenamientoRepository = planEntrenamientoRepository;
        this.userRepository = userRepository;
    }

    public List<RutinaResponse> listarRutinasPorPlan(Authentication authentication, String planId) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        PlanEntrenamiento plan = planEntrenamientoRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan de entrenamiento no encontrado"));

        validarAccesoLecturaPlan(usuarioActual, plan);

        return rutinaRepository.findByPlanIdAndActivoTrueOrderByOrdenAsc(planId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public List<RutinaResponse> listarRutinasPorPlanGestion(Authentication authentication, String planId) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        PlanEntrenamiento plan = planEntrenamientoRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan de entrenamiento no encontrado"));

        validarAccesoGestionPlan(usuarioActual, plan);

        return rutinaRepository.findByPlanIdOrderByOrdenAsc(planId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public RutinaResponse crearRutina(Authentication authentication, RutinaRequest request) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        validarRequestRutina(request, true);

        PlanEntrenamiento plan = planEntrenamientoRepository.findById(request.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plan de entrenamiento no encontrado"));

        validarAccesoGestionPlan(usuarioActual, plan);

        Rutina rutina = new Rutina();
        rutina.setPlanId(plan.getId());
        rutina.setNombre(request.getNombre().trim());
        rutina.setDescripcion(request.getDescripcion());
        rutina.setDiaSemana(normalizarDiaSemana(request.getDiaSemana()));
        rutina.setOrden(request.getOrden() != null ? request.getOrden() : 1);
        rutina.setActivo(request.getActivo() != null ? request.getActivo() : true);
        rutina.setFechaCreacion(LocalDateTime.now());
        rutina.setFechaActualizacion(LocalDateTime.now());

        Rutina guardada = rutinaRepository.save(rutina);

        return convertirAResponse(guardada);
    }

    public RutinaResponse actualizarRutina(Authentication authentication, String id, RutinaRequest request) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        validarRequestRutina(request, false);

        Rutina rutina = rutinaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rutina no encontrada"));

        PlanEntrenamiento plan = planEntrenamientoRepository.findById(rutina.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plan de entrenamiento no encontrado"));

        validarAccesoGestionPlan(usuarioActual, plan);

        rutina.setNombre(request.getNombre().trim());
        rutina.setDescripcion(request.getDescripcion());
        rutina.setDiaSemana(normalizarDiaSemana(request.getDiaSemana()));
        rutina.setOrden(request.getOrden() != null ? request.getOrden() : rutina.getOrden());

        if (request.getActivo() != null) {
            rutina.setActivo(request.getActivo());
        }

        rutina.setFechaActualizacion(LocalDateTime.now());

        Rutina actualizada = rutinaRepository.save(rutina);

        return convertirAResponse(actualizada);
    }

    public RutinaResponse cambiarEstadoRutina(Authentication authentication, String id, Boolean activo) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        Rutina rutina = rutinaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rutina no encontrada"));

        PlanEntrenamiento plan = planEntrenamientoRepository.findById(rutina.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plan de entrenamiento no encontrado"));

        validarAccesoGestionPlan(usuarioActual, plan);

        rutina.setActivo(activo != null ? activo : !Boolean.TRUE.equals(rutina.getActivo()));
        rutina.setFechaActualizacion(LocalDateTime.now());

        Rutina actualizada = rutinaRepository.save(rutina);

        return convertirAResponse(actualizada);
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

        throw new RuntimeException("No tienes permisos para ver las rutinas de este plan");
    }

    private void validarAccesoGestionPlan(UserDocument usuarioActual, PlanEntrenamiento plan) {
        if (tieneRol(usuarioActual, RoleName.ADMIN)) {
            return;
        }

        if (tieneRol(usuarioActual, RoleName.ENTRENADOR)
                && usuarioActual.getId().equals(plan.getEntrenadorId())) {
            return;
        }

        throw new RuntimeException("No tienes permisos para gestionar las rutinas de este plan");
    }

    private void validarRequestRutina(RutinaRequest request, boolean validarPlanId) {
        if (request == null) {
            throw new RuntimeException("Los datos de la rutina son obligatorios");
        }

        if (validarPlanId && esTextoVacio(request.getPlanId())) {
            throw new RuntimeException("El plan es obligatorio para crear la rutina");
        }

        if (esTextoVacio(request.getNombre())) {
            throw new RuntimeException("El nombre de la rutina es obligatorio");
        }

        if (esTextoVacio(request.getDiaSemana())) {
            throw new RuntimeException("El día de la semana es obligatorio");
        }

        String diaNormalizado = normalizarDiaSemana(request.getDiaSemana());

        if (diaNormalizado == null) {
            throw new RuntimeException("El día de la semana no es válido");
        }
    }

    private boolean esTextoVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    private String normalizarDiaSemana(String diaSemana) {
        if (diaSemana == null) {
            return null;
        }

        String dia = diaSemana
                .trim()
                .toLowerCase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u");

        switch (dia) {
            case "lunes":
                return "Lunes";
            case "martes":
                return "Martes";
            case "miercoles":
                return "Miércoles";
            case "jueves":
                return "Jueves";
            case "viernes":
                return "Viernes";
            case "sabado":
                return "Sábado";
            case "domingo":
                return "Domingo";
            default:
                return null;
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

    private RutinaResponse convertirAResponse(Rutina rutina) {
        RutinaResponse response = new RutinaResponse();
        response.setId(rutina.getId());
        response.setPlanId(rutina.getPlanId());
        response.setNombre(rutina.getNombre());
        response.setDescripcion(rutina.getDescripcion());
        response.setDiaSemana(rutina.getDiaSemana());
        response.setOrden(rutina.getOrden());
        response.setActivo(rutina.getActivo());
        response.setFechaCreacion(rutina.getFechaCreacion());
        response.setFechaActualizacion(rutina.getFechaActualizacion());
        return response;
    }
}