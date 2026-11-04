package com.ironfit.backendmongo.ironbot.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class GeminiIronBotClientTest {
    HttpClient http = mock(HttpClient.class);
    @SuppressWarnings("unchecked") HttpResponse<String> response = mock(HttpResponse.class);
    GeminiIronBotClient client = new GeminiIronBotClient("clave-de-prueba", "modelo-test", new ObjectMapper(), http);
    @Test void usaHeaderSinSecretoEnURLYTimeout() throws Exception {
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"Análisis\"}]}}]}");
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenAnswer(i -> {
            HttpRequest r = i.getArgument(0);
            assertEquals("clave-de-prueba", r.headers().firstValue("x-goog-api-key").orElseThrow());
            assertNull(r.uri().getQuery()); assertFalse(r.uri().toString().contains("clave-de-prueba"));
            assertEquals(Duration.ofSeconds(35), r.timeout().orElseThrow()); return response;
        });
        assertEquals("Análisis", client.generar("ADMIN", Map.of()));
    }
    @Test void rechazaErrorHTTP() throws Exception {
        when(response.statusCode()).thenReturn(429);
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        assertThrows(IllegalStateException.class, () -> client.generar("ADMIN", Map.of()));
    }
    @Test void rechazaRespuestaVaciaOBloqueada() throws Exception {
        when(response.statusCode()).thenReturn(200); when(response.body()).thenReturn("{\"candidates\":[]}");
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        assertThrows(IllegalStateException.class, () -> client.generar("ADMIN", Map.of()));
    }
    @Test void rechazaTextoVacio() throws Exception {
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[]}}]}");
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        assertThrows(IllegalStateException.class, () -> client.generar("ADMIN", Map.of()));
    }
}
