package com.ironfit.backendmongo.ironbot.servicio;

import com.ironfit.backendmongo.ironbot.dto.IronBotResponse;
import com.ironfit.backendmongo.prediccion.servicio.PrediccionPlanService;
import com.ironfit.backendmongo.prediccion.dto.PrediccionResponse;
import com.ironfit.backendmongo.repositorio.planes.PlanEntrenamientoRepository;
import com.ironfit.backendmongo.repositorio.seguridad.UserRepository;
import com.ironfit.backendmongo.repositorio.entrenamientos.EntrenamientoRealizadoRepository;
import com.ironfit.backendmongo.servicio.dashboard.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Service
public class IronBotService {
    private final GeminiIronBotClient gemini;
    private final PrediccionPlanService predicciones;
    private final PlanEntrenamientoRepository planes;
    private final UserRepository usuarios;
    private final EntrenamientoRealizadoRepository sesiones;
    private final DashboardService dashboard;
    public IronBotService(GeminiIronBotClient gemini, PrediccionPlanService predicciones,
            PlanEntrenamientoRepository planes, UserRepository usuarios,
            EntrenamientoRealizadoRepository sesiones, DashboardService dashboard) {
        this.gemini=gemini; this.predicciones=predicciones; this.planes=planes;
        this.usuarios=usuarios; this.sesiones=sesiones; this.dashboard=dashboard;
    }
    public IronBotResponse analizarPlan(String id, Authentication auth) {
        var usuario = usuarios.findByCorreo(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        var plan = planes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !usuario.getId().equals(plan.getEntrenadorId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este plan");
        if (!gemini.configurado()) return noDisponible("ANALISIS_PLAN");
        try {
            var p = predicciones.predecirPlan(id);
            var datos = new LinkedHashMap<String,Object>();
            // Se omiten textos libres (nombre, descripción, observaciones) y todos los IDs.
            datos.put("objetivo", objetivoSeguro(plan.getObjetivo()));
            datos.put("planActivo", plan.getActivo());
            datos.put("adherencia", prediccionSegura(p.getAdherencia()));
            datos.put("rendimiento", prediccionSegura(p.getRendimiento()));
            var registros = sesiones.findByPlanIdOrderByFechaDesc(id);
            datos.put("sesionesRegistradas", registros.size());
            datos.put("sesionesCompletadas", registros.stream().filter(s -> "COMPLETADO".equals(s.getEstado())).count());
            datos.put("sesionesParciales", registros.stream().filter(s -> "PARCIAL".equals(s.getEstado())).count());
            return generar("ENTRENADOR", "ANALISIS_PLAN", "Análisis del plan", datos, "IronFit + Weka");
        } catch (Exception e) { return fallo("ANALISIS_PLAN"); }
    }
    private String objetivoSeguro(String objetivo) {
        if (objetivo == null) return "No disponible";
        String valor = objetivo.trim().toLowerCase(Locale.ROOT);
        return switch (valor) {
            case "hipertrofia", "fuerza", "resistencia", "pérdida de peso", "perdida de peso",
                 "ganancia muscular", "salud", "mantenimiento", "acondicionamiento físico" -> valor;
            default -> "Objetivo personalizado (texto omitido por privacidad)";
        };
    }
    private Map<String,Object> prediccionSegura(PrediccionResponse p) {
        var datos = new LinkedHashMap<String,Object>();
        datos.put("estimacion", p.getPrediccion()); datos.put("modelo", p.getModelo());
        datos.put("probabilidades", p.getProbabilidades()); return datos;
    }
    public IronBotResponse resumenAdmin(Authentication auth) {
        if (!gemini.configurado()) return noDisponible("RESUMEN_ADMIN");
        try {
            var d = dashboard.obtenerDashboardAdmin(auth);
            var datos = new LinkedHashMap<String,Object>();
            datos.put("usuariosActivos", d.getUsuariosActivos()); datos.put("clientesActivos", d.getClientesActivos());
            datos.put("entrenadoresActivos", d.getEntrenadoresActivos()); datos.put("planesActivos", d.getPlanesActivos());
            datos.put("asistenciasMes", d.getAsistenciasMes()); datos.put("asistenciaGlobalPorcentaje", d.getAsistenciaGlobal());
            datos.put("evaluacionesRegistradas", d.getEvaluacionesRegistradas()); datos.put("clientesSinPlan", d.getClientesSinPlan());
            return generar("ADMIN", "RESUMEN_ADMIN", "Resumen operativo", datos, "IronFit");
        } catch (Exception e) { return fallo("RESUMEN_ADMIN"); }
    }
    public IronBotResponse resumenCliente(Authentication auth) {
        if (!gemini.configurado()) return noDisponible("RESUMEN_CLIENTE");
        try {
            var d = dashboard.obtenerDashboardCliente(auth);
            var datos = new LinkedHashMap<String,Object>();
            datos.put("planActivoDisponible", d.getPlanActual() != null && !"Sin plan activo".equals(d.getPlanActual()));
            datos.put("rutinasActivas", d.getRutinasActivas()); datos.put("sesionesMes", d.getSesionesMes());
            datos.put("rachaActual", d.getRachaActual()); datos.put("progresoPlanPorcentaje", d.getProgresoPlan());
            datos.put("significadoProgreso", "El dashboard mide el progreso como porcentaje de asistencia registrada; no representa avance temporal ni resultados físicos");
            return generar("CLIENTE", "RESUMEN_CLIENTE", "Tu progreso", datos, "IronFit");
        } catch (Exception e) { return fallo("RESUMEN_CLIENTE"); }
    }
    private IronBotResponse generar(String rol, String tipo, String titulo, Map<String,Object> datos, String fuente) {
        try { return new IronBotResponse(true, tipo, titulo, gemini.generar(rol, datos), gemini.modelo(), fuente); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return fallo(tipo); }
        catch (Exception e) { return fallo(tipo); }
    }
    private IronBotResponse noDisponible(String tipo) {
        return new IronBotResponse(false, tipo, "IronBot", "IronBot no está disponible en este entorno.", gemini.modelo(), "IronFit");
    }
    private IronBotResponse fallo(String tipo) {
        return new IronBotResponse(false, tipo, "IronBot", "No se pudo completar el análisis. Inténtalo de nuevo más tarde.", gemini.modelo(), "IronFit");
    }
}
