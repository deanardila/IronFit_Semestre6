package com.ironfit.backendmongo.servicio.rutinas;

import com.ironfit.backendmongo.dto.rutinas.RutinaEjercicioRequest;
import com.ironfit.backendmongo.dto.rutinas.RutinaEjercicioResponse;
import com.ironfit.backendmongo.modelo.ejercicios.Ejercicio;
import com.ironfit.backendmongo.modelo.planes.PlanEntrenamiento;
import com.ironfit.backendmongo.modelo.rutinas.Rutina;
import com.ironfit.backendmongo.modelo.rutinas.RutinaEjercicio;
import com.ironfit.backendmongo.modelo.seguridad.RoleName;
import com.ironfit.backendmongo.modelo.seguridad.UserDocument;
import com.ironfit.backendmongo.repositorio.ejercicios.EjercicioRepository;
import com.ironfit.backendmongo.repositorio.planes.PlanEntrenamientoRepository;
import com.ironfit.backendmongo.repositorio.rutinas.RutinaEjercicioRepository;
import com.ironfit.backendmongo.repositorio.rutinas.RutinaRepository;
import com.ironfit.backendmongo.repositorio.seguridad.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RutinaEjercicioService {

    private final RutinaEjercicioRepository rutinaEjercicioRepository;
    private final RutinaRepository rutinaRepository;
    private final PlanEntrenamientoRepository planEntrenamientoRepository;
    private final EjercicioRepository ejercicioRepository;
    private final UserRepository userRepository;

    public RutinaEjercicioService(
            RutinaEjercicioRepository rutinaEjercicioRepository,
            RutinaRepository rutinaRepository,
            PlanEntrenamientoRepository planEntrenamientoRepository,
            EjercicioRepository ejercicioRepository,
            UserRepository userRepository
    ) {
        this.rutinaEjercicioRepository = rutinaEjercicioRepository;
        this.rutinaRepository = rutinaRepository;
        this.planEntrenamientoRepository = planEntrenamientoRepository;
        this.ejercicioRepository = ejercicioRepository;
        this.userRepository = userRepository;
    }

    public List<RutinaEjercicioResponse> listarEjerciciosPorRutina(
            Authentication authentication,
            String rutinaId
    ) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        Rutina rutina = rutinaRepository.findById(rutinaId)
                .orElseThrow(() -> new RuntimeException("Rutina no encontrada"));

        PlanEntrenamiento plan = planEntrenamientoRepository.findById(rutina.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plan de entrenamiento no encontrado"));

        validarAccesoLecturaPlan(usuarioActual, plan);

        return rutinaEjercicioRepository.findByRutinaIdOrderByOrdenAsc(rutinaId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }
    
    public RutinaEjercicioResponse agregarEjercicioARutina(
        Authentication authentication,
        RutinaEjercicioRequest request
    ) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        validarRequest(request, true);

        Rutina rutina = rutinaRepository.findById(request.getRutinaId())
                .orElseThrow(() -> new RuntimeException("Rutina no encontrada"));

        PlanEntrenamiento plan = planEntrenamientoRepository.findById(rutina.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plan de entrenamiento no encontrado"));

        validarAccesoGestionPlan(usuarioActual, plan);

        ejercicioRepository.findById(request.getEjercicioId())
                .orElseThrow(() -> new RuntimeException("Ejercicio no encontrado"));

        boolean yaExiste = rutinaEjercicioRepository.existsByRutinaIdAndEjercicioId(
                request.getRutinaId(),
                request.getEjercicioId()
        );

        if (yaExiste) {
            throw new RuntimeException("Este ejercicio ya está asignado a la rutina");
        }

        RutinaEjercicio rutinaEjercicio = new RutinaEjercicio();
        rutinaEjercicio.setRutinaId(request.getRutinaId());
        rutinaEjercicio.setEjercicioId(request.getEjercicioId());
        rutinaEjercicio.setSeries(request.getSeries());
        rutinaEjercicio.setRepeticiones(request.getRepeticiones());
        rutinaEjercicio.setDescansoSegundos(request.getDescansoSegundos());
        rutinaEjercicio.setTiempoSegundos(request.getTiempoSegundos());
        rutinaEjercicio.setPesoSugerido(request.getPesoSugerido());
        rutinaEjercicio.setOrden(request.getOrden() != null ? request.getOrden() : 1);

        RutinaEjercicio guardado = rutinaEjercicioRepository.save(rutinaEjercicio);

        return convertirAResponse(guardado);
    }

    public RutinaEjercicioResponse actualizarEjercicioDeRutina(
            Authentication authentication,
            String id,
            RutinaEjercicioRequest request
    ) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        validarRequest(request, false);

        RutinaEjercicio rutinaEjercicio = rutinaEjercicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ejercicio de rutina no encontrado"));

        Rutina rutina = rutinaRepository.findById(rutinaEjercicio.getRutinaId())
                .orElseThrow(() -> new RuntimeException("Rutina no encontrada"));

        PlanEntrenamiento plan = planEntrenamientoRepository.findById(rutina.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plan de entrenamiento no encontrado"));

        validarAccesoGestionPlan(usuarioActual, plan);

        rutinaEjercicio.setSeries(request.getSeries());
        rutinaEjercicio.setRepeticiones(request.getRepeticiones());
        rutinaEjercicio.setDescansoSegundos(request.getDescansoSegundos());
        rutinaEjercicio.setTiempoSegundos(request.getTiempoSegundos());
        rutinaEjercicio.setPesoSugerido(request.getPesoSugerido());

        if (request.getOrden() != null) {
            rutinaEjercicio.setOrden(request.getOrden());
        }

        RutinaEjercicio actualizado = rutinaEjercicioRepository.save(rutinaEjercicio);

        return convertirAResponse(actualizado);
    }

    public void eliminarEjercicioDeRutina(
            Authentication authentication,
            String id
    ) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);

        RutinaEjercicio rutinaEjercicio = rutinaEjercicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ejercicio de rutina no encontrado"));

        Rutina rutina = rutinaRepository.findById(rutinaEjercicio.getRutinaId())
                .orElseThrow(() -> new RuntimeException("Rutina no encontrada"));

        PlanEntrenamiento plan = planEntrenamientoRepository.findById(rutina.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plan de entrenamiento no encontrado"));

        validarAccesoGestionPlan(usuarioActual, plan);

        rutinaEjercicioRepository.deleteById(id);
    }
        private RutinaEjercicioResponse convertirAResponse(RutinaEjercicio rutinaEjercicio) {
            RutinaEjercicioResponse response = new RutinaEjercicioResponse();

            response.setId(rutinaEjercicio.getId());
            response.setRutinaId(rutinaEjercicio.getRutinaId());
            response.setEjercicioId(rutinaEjercicio.getEjercicioId());

            response.setSeries(rutinaEjercicio.getSeries());
            response.setRepeticiones(rutinaEjercicio.getRepeticiones());
            response.setDescansoSegundos(rutinaEjercicio.getDescansoSegundos());
            response.setTiempoSegundos(rutinaEjercicio.getTiempoSegundos());
            response.setPesoSugerido(rutinaEjercicio.getPesoSugerido());
            response.setOrden(rutinaEjercicio.getOrden());

            ejercicioRepository.findById(rutinaEjercicio.getEjercicioId())
                    .ifPresentOrElse(
                            ejercicio -> llenarDatosEjercicio(response, ejercicio),
                            () -> {
                                response.setEjercicioNombre("Ejercicio no encontrado");
                                response.setEjercicioDescripcion("No se encontró información del ejercicio asignado.");
                            }
                    );

            return response;
    }

    private void llenarDatosEjercicio(RutinaEjercicioResponse response, Ejercicio ejercicio) {
        response.setEjercicioNombre(ejercicio.getNombre());
        response.setEjercicioDescripcion(ejercicio.getDescripcion());
        response.setCategoria(ejercicio.getCategoria());
        response.setGrupoMuscular(ejercicio.getGrupoMuscular());
        response.setTipoEquipo(ejercicio.getTipoEquipo());
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

        throw new RuntimeException("No tienes permisos para ver los ejercicios de esta rutina");
    }


    private void validarAccesoGestionPlan(UserDocument usuarioActual, PlanEntrenamiento plan) {
    if (tieneRol(usuarioActual, RoleName.ENTRENADOR)
            && usuarioActual.getId().equals(plan.getEntrenadorId())) {
        return;
    }

    throw new RuntimeException("No tienes permisos para gestionar ejercicios en esta rutina");
    }

    private void validarRequest(RutinaEjercicioRequest request, boolean validarRutinaYEjercicio) {
        if (request == null) {
            throw new RuntimeException("Los datos del ejercicio de rutina son obligatorios");
        }

        if (validarRutinaYEjercicio && esTextoVacio(request.getRutinaId())) {
            throw new RuntimeException("La rutina es obligatoria");
        }

        if (validarRutinaYEjercicio && esTextoVacio(request.getEjercicioId())) {
            throw new RuntimeException("El ejercicio es obligatorio");
        }

        if (request.getSeries() == null || request.getSeries() < 0) {
            throw new RuntimeException("Las series deben ser mayor o igual a 0");
        }

        if (request.getRepeticiones() == null || request.getRepeticiones() < 0) {
            throw new RuntimeException("Las repeticiones deben ser mayor o igual a 0");
        }

        if (request.getOrden() != null && request.getOrden() < 1) {
            throw new RuntimeException("El orden debe ser mayor o igual a 1");
        }
    }

    private boolean esTextoVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
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