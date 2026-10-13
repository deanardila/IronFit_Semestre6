package com.ironfit.backendmongo.servicio.dashboard;

import com.ironfit.backendmongo.dto.dashboard.DashboardAdminResponse;
import com.ironfit.backendmongo.dto.dashboard.DashboardClienteResponse;
import com.ironfit.backendmongo.dto.dashboard.DashboardEntrenadorResponse;
import com.ironfit.backendmongo.dto.dashboard.GraficoDatoResponse;
import com.ironfit.backendmongo.modelo.asignaciones.AsignacionEntrenadorCliente;
import com.ironfit.backendmongo.modelo.asistencias.Asistencia;
import com.ironfit.backendmongo.modelo.evaluaciones.EvaluacionFisica;
import com.ironfit.backendmongo.modelo.evaluaciones.MedidasCorporales;
import com.ironfit.backendmongo.modelo.planes.PlanEntrenamiento;
import com.ironfit.backendmongo.modelo.seguridad.RoleName;
import com.ironfit.backendmongo.modelo.seguridad.UserDocument;
import com.ironfit.backendmongo.repositorio.asignaciones.AsignacionEntrenadorClienteRepository;
import com.ironfit.backendmongo.repositorio.asistencias.AsistenciaRepository;
import com.ironfit.backendmongo.repositorio.evaluaciones.EvaluacionFisicaRepository;
import com.ironfit.backendmongo.repositorio.planes.PlanEntrenamientoRepository;
import com.ironfit.backendmongo.repositorio.rutinas.RutinaRepository;
import com.ironfit.backendmongo.repositorio.seguridad.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final PlanEntrenamientoRepository planRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final RutinaRepository rutinaRepository;
    private final AsignacionEntrenadorClienteRepository asignacionRepository;
    private final EvaluacionFisicaRepository evaluacionRepository;

    public DashboardService(
            UserRepository userRepository,
            PlanEntrenamientoRepository planRepository,
            AsistenciaRepository asistenciaRepository,
            RutinaRepository rutinaRepository,
            AsignacionEntrenadorClienteRepository asignacionRepository,
            EvaluacionFisicaRepository evaluacionRepository
    ) {
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.rutinaRepository = rutinaRepository;
        this.asignacionRepository = asignacionRepository;
        this.evaluacionRepository = evaluacionRepository;
    }

    public DashboardAdminResponse obtenerDashboardAdmin(Authentication authentication) {
        UserDocument usuarioActual = obtenerUsuarioAutenticado(authentication);
        validarRol(usuarioActual, RoleName.ADMIN, "Solo el administrador puede ver este dashboard");

        List<UserDocument> usuarios = userRepository.findAll();
        List<PlanEntrenamiento> planes = planRepository.findAllByOrderByFechaCreacionDesc();
        List<Asistencia> asistencias = asistenciaRepository.findAllByOrderByFechaAsistenciaDesc();
        List<EvaluacionFisica> evaluaciones = evaluacionRepository.findAllByOrderByFechaDesc();

        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate hoy = LocalDate.now();

        DashboardAdminResponse response = new DashboardAdminResponse();

        response.setUsuariosActivos(usuarios.stream().filter(UserDocument::isActivo).count());
        response.setClientesActivos(usuarios.stream().filter(u -> u.isActivo() && tieneRol(u, RoleName.CLIENTE)).count());
        response.setEntrenadoresActivos(usuarios.stream().filter(u -> u.isActivo() && tieneRol(u, RoleName.ENTRENADOR)).count());
        response.setPlanesActivos(planes.stream().filter(p -> Boolean.TRUE.equals(p.getActivo())).count());

        List<Asistencia> asistenciasMes = asistencias.stream()
                .filter(a -> a.getFechaAsistencia() != null)
                .filter(a -> !a.getFechaAsistencia().isBefore(inicioMes) && !a.getFechaAsistencia().isAfter(hoy))
                .toList();

        response.setAsistenciasMes((long) asistenciasMes.size());
        response.setAsistenciaGlobal(calcularPorcentajeAsistencia(asistenciasMes));
        response.setEvaluacionesRegistradas((long) evaluaciones.size());
        response.setClientesSinPlan(calcularClientesSinPlan(usuarios, planes));

        response.setUsuariosPorRol(construirUsuariosPorRol(usuarios));
        response.setPlanesPorObjetivo(construirGraficoPorTexto(
                planes,
                plan -> textoSeguro(plan.getObjetivo(), "Sin objetivo")
        ));

        response.setPlanesPorEntrenador(construirGraficoPorTexto(
                planes,
                plan -> obtenerNombreUsuario(plan.getEntrenadorId())
        ));

        response.setAsistenciaUltimosDias(construirAsistenciaUltimosDias(asistencias, 14));

        return response;
    }

    public DashboardEntrenadorResponse obtenerDashboardEntrenador(Authentication authentication) {
        UserDocument entrenador = obtenerUsuarioAutenticado(authentication);
        validarRol(entrenador, RoleName.ENTRENADOR, "Solo el entrenador puede ver este dashboard");

        List<AsignacionEntrenadorCliente> asignaciones =
                asignacionRepository.findByEntrenadorIdAndActivoTrue(entrenador.getId());

        List<String> clienteIds = asignaciones.stream()
                .map(AsignacionEntrenadorCliente::getClienteId)
                .distinct()
                .toList();

        List<PlanEntrenamiento> planes = planRepository.findByEntrenadorIdOrderByFechaCreacionDesc(entrenador.getId());
        List<String> planIds = planes.stream().map(PlanEntrenamiento::getId).toList();

        List<Asistencia> asistencias = asistenciaRepository.findAllByOrderByFechaAsistenciaDesc()
                .stream()
                .filter(a -> planIds.contains(a.getPlanId()))
                .toList();

        List<EvaluacionFisica> evaluaciones = evaluacionRepository.findByEntrenadorIdOrderByFechaDesc(entrenador.getId());

        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate hoy = LocalDate.now();

        List<Asistencia> asistenciasMes = asistencias.stream()
                .filter(a -> a.getFechaAsistencia() != null)
                .filter(a -> !a.getFechaAsistencia().isBefore(inicioMes) && !a.getFechaAsistencia().isAfter(hoy))
                .toList();

        DashboardEntrenadorResponse response = new DashboardEntrenadorResponse();

        response.setClientesActivos((long) clienteIds.size());
        response.setPlanesActivos(planes.stream().filter(p -> Boolean.TRUE.equals(p.getActivo())).count());
        response.setSesionesMes(asistenciasMes.stream().filter(a -> Boolean.TRUE.equals(a.getAsistio())).count());
        response.setCumplimientoMes(calcularPorcentajeAsistencia(asistenciasMes));
        response.setRutinasActivas(contarRutinasActivas(planes));

        Set<String> clientesEvaluados = evaluaciones.stream()
                .map(EvaluacionFisica::getClienteId)
                .collect(Collectors.toSet());

        response.setEvaluacionesPendientes(
                clienteIds.stream().filter(id -> !clientesEvaluados.contains(id)).count()
        );

        response.setAsistenciaUltimosDias(construirAsistenciaUltimosDias(asistencias, 14));
        response.setProgresoPorCliente(construirProgresoPorCliente(asistencias, clienteIds));
        response.setPlanesPorEstado(construirPlanesPorEstado(planes));
        response.setEvaluacionesPorCliente(construirEvaluacionesPorCliente(evaluaciones));

        return response;
    }

    public DashboardClienteResponse obtenerDashboardCliente(Authentication authentication) {
        UserDocument cliente = obtenerUsuarioAutenticado(authentication);
        validarRol(cliente, RoleName.CLIENTE, "Solo el cliente puede ver este dashboard");

        List<PlanEntrenamiento> planesCliente = planRepository.findByClienteIdOrderByFechaCreacionDesc(cliente.getId());
        Optional<PlanEntrenamiento> planActivo = planesCliente.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .findFirst();

        List<Asistencia> asistencias = asistenciaRepository.findByClienteIdOrderByFechaAsistenciaDesc(cliente.getId());
        List<EvaluacionFisica> evaluaciones = evaluacionRepository.findByClienteIdOrderByFechaDesc(cliente.getId());

        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate hoy = LocalDate.now();

        List<Asistencia> asistenciasMes = asistencias.stream()
                .filter(a -> a.getFechaAsistencia() != null)
                .filter(a -> !a.getFechaAsistencia().isBefore(inicioMes) && !a.getFechaAsistencia().isAfter(hoy))
                .toList();

        DashboardClienteResponse response = new DashboardClienteResponse();

        response.setPlanActual(planActivo.map(PlanEntrenamiento::getNombre).orElse("Sin plan activo"));
        response.setRutinasActivas(planActivo
                .map(plan -> (long) rutinaRepository.findByPlanIdAndActivoTrueOrderByOrdenAsc(plan.getId()).size())
                .orElse(0L));

        response.setSesionesMes(asistenciasMes.stream().filter(a -> Boolean.TRUE.equals(a.getAsistio())).count());
        response.setRachaActual(calcularRachaActual(asistencias));
        response.setProgresoPlan(calcularPorcentajeAsistencia(asistencias));
        response.setAsistenciaUltimosDias(construirAsistenciaUltimosDias(asistencias, 14));
        response.setEvolucionPeso(construirEvolucionPeso(evaluaciones));
        response.setProgresoPlanGrafico(List.of(
                new GraficoDatoResponse("Cumplido", response.getProgresoPlan()),
                new GraficoDatoResponse("Pendiente", Math.max(0, 100 - response.getProgresoPlan()))
        ));

        if (!evaluaciones.isEmpty()) {
            EvaluacionFisica ultima = evaluaciones.get(0);
            response.setPesoActual(ultima.getPesoCorporal());
            response.setImcActual(ultima.getImc());
            response.setMetricasCorporales(construirMetricasCorporales(ultima));
        } else {
            response.setPesoActual(0.0);
            response.setImcActual(0.0);
            response.setMetricasCorporales(List.of());
        }

        return response;
    }

    private Long calcularClientesSinPlan(List<UserDocument> usuarios, List<PlanEntrenamiento> planes) {
        Set<String> clientesConPlanActivo = planes.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .map(PlanEntrenamiento::getClienteId)
                .collect(Collectors.toSet());

        return usuarios.stream()
                .filter(u -> u.isActivo() && tieneRol(u, RoleName.CLIENTE))
                .filter(u -> !clientesConPlanActivo.contains(u.getId()))
                .count();
    }

    private Long contarRutinasActivas(List<PlanEntrenamiento> planes) {
        return planes.stream()
                .flatMap(plan -> rutinaRepository.findByPlanIdAndActivoTrueOrderByOrdenAsc(plan.getId()).stream())
                .count();
    }

    private Double calcularPorcentajeAsistencia(List<Asistencia> asistencias) {
        if (asistencias == null || asistencias.isEmpty()) {
            return 0.0;
        }

        long asistio = asistencias.stream()
                .filter(a -> Boolean.TRUE.equals(a.getAsistio()))
                .count();

        return redondear((asistio * 100.0) / asistencias.size());
    }

    private Long calcularRachaActual(List<Asistencia> asistencias) {
        Map<LocalDate, Boolean> mapa = asistencias.stream()
                .filter(a -> a.getFechaAsistencia() != null)
                .collect(Collectors.toMap(
                        Asistencia::getFechaAsistencia,
                        a -> Boolean.TRUE.equals(a.getAsistio()),
                        (a, b) -> a || b
                ));

        long racha = 0;
        LocalDate fecha = LocalDate.now();

        while (Boolean.TRUE.equals(mapa.get(fecha))) {
            racha++;
            fecha = fecha.minusDays(1);
        }

        return racha;
    }

    private List<GraficoDatoResponse> construirAsistenciaUltimosDias(List<Asistencia> asistencias, int dias) {
        List<GraficoDatoResponse> datos = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (int i = dias - 1; i >= 0; i--) {
            LocalDate fecha = hoy.minusDays(i);

            List<Asistencia> delDia = asistencias.stream()
                    .filter(a -> fecha.equals(a.getFechaAsistencia()))
                    .toList();

            datos.add(new GraficoDatoResponse(
                    fecha.format(formatter),
                    calcularPorcentajeAsistencia(delDia)
            ));
        }

        return datos;
    }

    private List<GraficoDatoResponse> construirUsuariosPorRol(List<UserDocument> usuarios) {
        return List.of(
                new GraficoDatoResponse("Clientes", (double) usuarios.stream().filter(u -> tieneRol(u, RoleName.CLIENTE)).count()),
                new GraficoDatoResponse("Entrenadores", (double) usuarios.stream().filter(u -> tieneRol(u, RoleName.ENTRENADOR)).count()),
                new GraficoDatoResponse("Admins", (double) usuarios.stream().filter(u -> tieneRol(u, RoleName.ADMIN)).count())
        );
    }

    private List<GraficoDatoResponse> construirPlanesPorEstado(List<PlanEntrenamiento> planes) {
        long activos = planes.stream().filter(p -> Boolean.TRUE.equals(p.getActivo())).count();
        long inactivos = planes.size() - activos;

        return List.of(
                new GraficoDatoResponse("Activos", (double) activos),
                new GraficoDatoResponse("Inactivos", (double) inactivos)
        );
    }

    private List<GraficoDatoResponse> construirProgresoPorCliente(List<Asistencia> asistencias, List<String> clienteIds) {
        return clienteIds.stream()
                .map(clienteId -> {
                    List<Asistencia> asistenciasCliente = asistencias.stream()
                            .filter(a -> clienteId.equals(a.getClienteId()))
                            .toList();

                    return new GraficoDatoResponse(
                            obtenerNombreUsuario(clienteId),
                            calcularPorcentajeAsistencia(asistenciasCliente)
                    );
                })
                .sorted((a, b) -> Double.compare(b.getValor(), a.getValor()))
                .limit(6)
                .toList();
    }

    private List<GraficoDatoResponse> construirEvaluacionesPorCliente(List<EvaluacionFisica> evaluaciones) {
        Map<String, Long> conteo = evaluaciones.stream()
                .collect(Collectors.groupingBy(EvaluacionFisica::getClienteId, Collectors.counting()));

        return conteo.entrySet()
                .stream()
                .map(e -> new GraficoDatoResponse(obtenerNombreUsuario(e.getKey()), e.getValue().doubleValue()))
                .sorted((a, b) -> Double.compare(b.getValor(), a.getValor()))
                .limit(6)
                .toList();
    }

    private List<GraficoDatoResponse> construirEvolucionPeso(List<EvaluacionFisica> evaluaciones) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        return evaluaciones.stream()
                .filter(e -> e.getFecha() != null && e.getPesoCorporal() != null)
                .sorted(Comparator.comparing(EvaluacionFisica::getFecha))
                .limit(8)
                .map(e -> new GraficoDatoResponse(
                        e.getFecha().format(formatter),
                        e.getPesoCorporal()
                ))
                .toList();
    }

    private List<GraficoDatoResponse> construirMetricasCorporales(EvaluacionFisica evaluacion) {
        MedidasCorporales medidas = evaluacion.getMedidasCorporales();

        if (medidas == null) {
            return List.of();
        }

        return List.of(
                new GraficoDatoResponse("Pecho", medidas.getPecho()),
                new GraficoDatoResponse("Cintura", medidas.getCintura()),
                new GraficoDatoResponse("Brazo", medidas.getBrazo()),
                new GraficoDatoResponse("Pierna", medidas.getPierna())
        );
    }

    private <T> List<GraficoDatoResponse> construirGraficoPorTexto(
            List<T> lista,
            java.util.function.Function<T, String> selector
    ) {
        Map<String, Long> conteo = lista.stream()
                .collect(Collectors.groupingBy(selector, Collectors.counting()));

        return conteo.entrySet()
                .stream()
                .map(e -> new GraficoDatoResponse(e.getKey(), e.getValue().doubleValue()))
                .sorted((a, b) -> Double.compare(b.getValor(), a.getValor()))
                .limit(6)
                .toList();
    }

    private UserDocument obtenerUsuarioAutenticado(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("No se pudo identificar el usuario autenticado");
        }

        return userRepository.findByCorreo(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
    }

    private void validarRol(UserDocument usuario, String rol, String mensaje) {
        if (!tieneRol(usuario, rol)) {
            throw new RuntimeException(mensaje);
        }
    }

    private boolean tieneRol(UserDocument usuario, String rol) {
        return usuario.getRoles() != null && usuario.getRoles().contains(rol);
    }

    private String obtenerNombreUsuario(String usuarioId) {
        return userRepository.findById(usuarioId)
                .map(this::construirNombreCompleto)
                .orElse("No encontrado");
    }

    private String construirNombreCompleto(UserDocument usuario) {
        String nombres = usuario.getNombres() != null ? usuario.getNombres().trim() : "";
        String apellidos = usuario.getApellidos() != null ? usuario.getApellidos().trim() : "";
        return (nombres + " " + apellidos).trim();
    }

    private String textoSeguro(String valor, String reemplazo) {
        return valor != null && !valor.trim().isEmpty() ? valor.trim() : reemplazo;
    }

    private Double redondear(Double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}