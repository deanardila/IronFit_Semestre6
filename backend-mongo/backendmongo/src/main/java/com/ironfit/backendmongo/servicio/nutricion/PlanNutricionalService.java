package com.ironfit.backendmongo.servicio.nutricion;

import com.ironfit.backendmongo.dto.nutricion.PlanNutricionalRequest;
import com.ironfit.backendmongo.dto.nutricion.PlanNutricionalResponse;
import com.ironfit.backendmongo.modelo.nutricion.PlanNutricional;
import com.ironfit.backendmongo.modelo.planes.PlanEntrenamiento;
import com.ironfit.backendmongo.modelo.seguridad.RoleName;
import com.ironfit.backendmongo.modelo.seguridad.UserDocument;
import com.ironfit.backendmongo.repositorio.nutricion.PlanNutricionalRepository;
import com.ironfit.backendmongo.repositorio.planes.PlanEntrenamientoRepository;
import com.ironfit.backendmongo.repositorio.seguridad.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PlanNutricionalService {

    private final PlanNutricionalRepository nutricionalRepository;
    private final PlanEntrenamientoRepository planRepository;
    private final UserRepository userRepository;

    public PlanNutricionalService(
            PlanNutricionalRepository nutricionalRepository,
            PlanEntrenamientoRepository planRepository,
            UserRepository userRepository
    ) {
        this.nutricionalRepository = nutricionalRepository;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
    }

    public List<PlanNutricionalResponse> listar(Authentication authentication) {
        UserDocument usuario = obtenerUsuario(authentication);

        List<PlanNutricional> planes;

        if (tieneRol(usuario, RoleName.ADMIN)) {
            planes = nutricionalRepository.findAll();
        } else if (tieneRol(usuario, RoleName.ENTRENADOR)) {
            planes = nutricionalRepository.findByEntrenadorIdOrderByFechaActualizacionDesc(usuario.getId());
        } else if (tieneRol(usuario, RoleName.CLIENTE)) {
            planes = nutricionalRepository.findByClienteIdOrderByFechaActualizacionDesc(usuario.getId());
        } else {
            throw new RuntimeException("No tienes permisos para consultar planes nutricionales");
        }

        return planes.stream()
                .filter(this::esRegistroNutricionalUtil)
                .map(this::convertir)
                .toList();
    }

    public PlanNutricionalResponse obtenerPorPlan(Authentication authentication, String planId) {
        UserDocument usuario = obtenerUsuario(authentication);

        PlanEntrenamiento plan = obtenerPlan(planId);
        validarLectura(usuario, plan);

        PlanNutricional nutricional = nutricionalRepository.findByPlanId(planId)
                .orElseThrow(() -> new RuntimeException("El plan seleccionado no tiene un plan nutricional registrado"));

        return convertir(nutricional);
    }

    public PlanNutricionalResponse crear(Authentication authentication, PlanNutricionalRequest request) {
        UserDocument entrenador = obtenerUsuario(authentication);

        if (!tieneRol(entrenador, RoleName.ENTRENADOR)) {
            throw new RuntimeException("Solo el entrenador puede crear planes nutricionales");
        }

        PlanEntrenamiento plan = obtenerPlan(request.getPlanId());
        validarPlanDelEntrenador(entrenador, plan);

        if (nutricionalRepository.findByPlanId(plan.getId()).isPresent()) {
            throw new RuntimeException("Este plan de entrenamiento ya tiene un plan nutricional");
        }

        LocalDateTime ahora = LocalDateTime.now();

        PlanNutricional nutricional = new PlanNutricional();
        nutricional.setPlanId(plan.getId());
        nutricional.setClienteId(plan.getClienteId());
        nutricional.setEntrenadorId(plan.getEntrenadorId());
        aplicarDatos(nutricional, request);
        nutricional.setFechaCreacion(ahora);
        nutricional.setFechaActualizacion(ahora);

        return convertir(nutricionalRepository.save(nutricional));
    }

    public PlanNutricionalResponse actualizar(
            Authentication authentication,
            String id,
            PlanNutricionalRequest request
    ) {
        UserDocument entrenador = obtenerUsuario(authentication);

        if (!tieneRol(entrenador, RoleName.ENTRENADOR)) {
            throw new RuntimeException("Solo el entrenador puede actualizar planes nutricionales");
        }

        PlanNutricional nutricional = nutricionalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan nutricional no encontrado"));

        PlanEntrenamiento plan = obtenerPlan(nutricional.getPlanId());
        validarPlanDelEntrenador(entrenador, plan);

        if (!nutricional.getPlanId().equals(request.getPlanId())) {
            throw new RuntimeException("No se puede cambiar el plan de entrenamiento asociado");
        }

        aplicarDatos(nutricional, request);
        nutricional.setFechaActualizacion(LocalDateTime.now());

        return convertir(nutricionalRepository.save(nutricional));
    }

    private void aplicarDatos(PlanNutricional nutricional, PlanNutricionalRequest request) {
        nutricional.setKcalObjetivo(request.getKcalObjetivo());
        nutricional.setProteinaG(request.getProteinaG());
        nutricional.setCarbohidratosG(request.getCarbohidratosG());
        nutricional.setGrasaG(request.getGrasaG());
        nutricional.setDescripcion(request.getDescripcion().trim());
    }

    private PlanEntrenamiento obtenerPlan(String planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan de entrenamiento no encontrado"));
    }

    private void validarPlanDelEntrenador(UserDocument entrenador, PlanEntrenamiento plan) {
        if (!entrenador.getId().equals(plan.getEntrenadorId())) {
            throw new RuntimeException("No puedes gestionar nutrición de un plan que no te pertenece");
        }
    }

    private void validarLectura(UserDocument usuario, PlanEntrenamiento plan) {
        if (tieneRol(usuario, RoleName.ADMIN)) return;

        if (tieneRol(usuario, RoleName.ENTRENADOR)
                && usuario.getId().equals(plan.getEntrenadorId())) return;

        if (tieneRol(usuario, RoleName.CLIENTE)
                && usuario.getId().equals(plan.getClienteId())) return;

        throw new RuntimeException("No tienes permisos para consultar este plan nutricional");
    }

    private UserDocument obtenerUsuario(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("No se pudo identificar el usuario autenticado");
        }

        return userRepository.findByCorreo(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
    }

    private boolean tieneRol(UserDocument usuario, String rol) {
        return usuario.getRoles() != null && usuario.getRoles().contains(rol);
    }

    private boolean esRegistroNutricionalUtil(PlanNutricional nutricional) {
        if (nutricional == null) {
            return false;
        }

        if (nutricional.getPlanId() == null || nutricional.getPlanId().isBlank()) {
            return false;
        }

        return nutricional.getKcalObjetivo() != null
                || nutricional.getProteinaG() != null
                || nutricional.getCarbohidratosG() != null
                || nutricional.getGrasaG() != null
                || (nutricional.getDescripcion() != null && !nutricional.getDescripcion().isBlank());
    }

    private PlanNutricionalResponse convertir(PlanNutricional nutricional) {
        PlanNutricionalResponse response = new PlanNutricionalResponse();
        response.setId(nutricional.getId());
        response.setPlanId(nutricional.getPlanId());
        response.setClienteId(nutricional.getClienteId());
        response.setEntrenadorId(nutricional.getEntrenadorId());
        response.setKcalObjetivo(nutricional.getKcalObjetivo());
        response.setProteinaG(nutricional.getProteinaG());
        response.setCarbohidratosG(nutricional.getCarbohidratosG());
        response.setGrasaG(nutricional.getGrasaG());
        response.setDescripcion(nutricional.getDescripcion());
        response.setFechaCreacion(nutricional.getFechaCreacion());
        response.setFechaActualizacion(nutricional.getFechaActualizacion());

        if (nutricional.getPlanId() != null && !nutricional.getPlanId().isBlank()) {
            planRepository.findById(nutricional.getPlanId()).ifPresent(plan ->
                    response.setPlanNombre(plan.getNombre()));
        }

        if (nutricional.getClienteId() != null && !nutricional.getClienteId().isBlank()) {
            userRepository.findById(nutricional.getClienteId()).ifPresent(cliente ->
                    response.setClienteNombre(nombreCompleto(cliente)));
        }

        if (nutricional.getEntrenadorId() != null && !nutricional.getEntrenadorId().isBlank()) {
            userRepository.findById(nutricional.getEntrenadorId()).ifPresent(entrenador ->
                    response.setEntrenadorNombre(nombreCompleto(entrenador)));
        }

        return response;
    }

    private String nombreCompleto(UserDocument usuario) {
        String nombres = usuario.getNombres() == null ? "" : usuario.getNombres().trim();
        String apellidos = usuario.getApellidos() == null ? "" : usuario.getApellidos().trim();
        return (nombres + " " + apellidos).trim();
    }
}
