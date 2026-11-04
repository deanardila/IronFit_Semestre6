package com.ironfit.backendmongo.ironbot;

import com.ironfit.backendmongo.ironbot.servicio.*;
import com.ironfit.backendmongo.prediccion.servicio.PrediccionPlanService;
import com.ironfit.backendmongo.prediccion.dto.*;
import com.ironfit.backendmongo.repositorio.planes.PlanEntrenamientoRepository;
import com.ironfit.backendmongo.repositorio.seguridad.UserRepository;
import com.ironfit.backendmongo.repositorio.entrenamientos.EntrenamientoRealizadoRepository;
import com.ironfit.backendmongo.modelo.seguridad.UserDocument;
import com.ironfit.backendmongo.modelo.planes.PlanEntrenamiento;
import com.ironfit.backendmongo.dto.dashboard.*;
import com.ironfit.backendmongo.servicio.dashboard.DashboardService;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class IronBotServiceTest {
    GeminiIronBotClient gemini = mock(GeminiIronBotClient.class);
    PrediccionPlanService predicciones = mock(PrediccionPlanService.class);
    PlanEntrenamientoRepository planes = mock(PlanEntrenamientoRepository.class);
    UserRepository usuarios = mock(UserRepository.class);
    EntrenamientoRealizadoRepository sesiones = mock(EntrenamientoRealizadoRepository.class);
    DashboardService dashboard = mock(DashboardService.class);
    IronBotService service = new IronBotService(gemini, predicciones, planes, usuarios, sesiones, dashboard);
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("privado@example.com", "jwt",
        List.of(new SimpleGrantedAuthority("ROLE_ENTRENADOR")));
    @BeforeEach void preparar() {
        var usuario = mock(UserDocument.class); when(usuario.getId()).thenReturn("entrenador");
        var plan = new PlanEntrenamiento(); plan.setEntrenadorId("entrenador"); plan.setClienteId("cliente-secreto");
        plan.setObjetivo("Hipertrofia"); plan.setActivo(true);
        when(usuarios.findByCorreo(auth.getName())).thenReturn(Optional.of(usuario));
        when(planes.findById("plan-secreto")).thenReturn(Optional.of(plan));
        when(gemini.modelo()).thenReturn("modelo-test");
    }
    @Test void rechazaPlanAjenoAntesDePredecirOConsultarIA() {
        var otro = new PlanEntrenamiento(); otro.setEntrenadorId("otro");
        when(planes.findById("plan-secreto")).thenReturn(Optional.of(otro));
        var e = assertThrows(ResponseStatusException.class, () -> service.analizarPlan("plan-secreto", auth));
        assertEquals(403, e.getStatusCode().value());
        verifyNoInteractions(predicciones, gemini, sesiones);
    }
    @Test void devuelve404ParaPlanInexistente() {
        when(planes.findById("plan-secreto")).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class,
            () -> service.analizarPlan("plan-secreto", auth)).getStatusCode().value());
    }
    @Test void sinClaveNoEjecutaPrediccionNiConsultaDashboard() {
        assertFalse(service.analizarPlan("plan-secreto", auth).disponible());
        assertFalse(service.resumenAdmin(auth).disponible());
        assertFalse(service.resumenCliente(auth).disponible());
        verifyNoInteractions(predicciones, dashboard, sesiones);
    }
    @Test void contextoPlanExcluyeIdentificadoresYUsaWeka() throws Exception {
        when(gemini.configurado()).thenReturn(true);
        var p = new PrediccionResponse("cliente-secreto", "plan-secreto", "ADHERENCIA", "MEDIA", "J48", Map.of("MEDIA", .7));
        when(predicciones.predecirPlan("plan-secreto")).thenReturn(new PrediccionPlanResponse("cliente-secreto", "plan-secreto", p, p));
        when(sesiones.findByPlanIdOrderByFechaDesc("plan-secreto")).thenReturn(List.of());
        when(gemini.generar(eq("ENTRENADOR"), anyMap())).thenAnswer(i -> {
            String contexto = i.getArgument(1).toString();
            assertFalse(contexto.contains("secreto")); assertFalse(contexto.contains("example.com"));
            assertTrue(contexto.contains("J48")); assertTrue(contexto.contains("0.7")); return "Estimación del plan";
        });
        var respuesta = service.analizarPlan("plan-secreto", auth);
        assertTrue(respuesta.disponible()); assertEquals("IronFit + Weka", respuesta.fuente());
        verify(predicciones).predecirPlan("plan-secreto");
    }
    @Test void clienteUsaSoloDashboardPropioSinNombreDelPlan() throws Exception {
        when(gemini.configurado()).thenReturn(true);
        var d = new DashboardClienteResponse(); d.setPlanActual("Nombre privado"); d.setSesionesMes(3L);
        when(dashboard.obtenerDashboardCliente(auth)).thenReturn(d);
        when(gemini.generar(eq("CLIENTE"), anyMap())).thenAnswer(i -> {
            String contexto = i.getArgument(1).toString(); assertFalse(contexto.contains("privado"));
            assertTrue(contexto.contains("sesionesMes=3")); return "Sigue las indicaciones de tu entrenador";
        });
        assertTrue(service.resumenCliente(auth).disponible());
        verify(dashboard).obtenerDashboardCliente(auth);
    }
    @Test void falloDeGeminiProduceRespuestaControlada() throws Exception {
        when(gemini.configurado()).thenReturn(true);
        when(dashboard.obtenerDashboardAdmin(auth)).thenReturn(new DashboardAdminResponse());
        when(gemini.generar(eq("ADMIN"), anyMap())).thenThrow(new java.net.http.HttpTimeoutException("timeout"));
        var respuesta = service.resumenAdmin(auth);
        assertFalse(respuesta.disponible()); assertFalse(respuesta.mensaje().contains("timeout"));
    }
}
