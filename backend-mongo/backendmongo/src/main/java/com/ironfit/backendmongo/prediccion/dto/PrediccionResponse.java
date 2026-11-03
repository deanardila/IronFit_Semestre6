package com.ironfit.backendmongo.prediccion.dto;

import java.util.Map;

public class PrediccionResponse {

    private String clienteId;
    private String planId;
    private String tipo;
    private String prediccion;
    private String modelo;
    private Map<String, Double> probabilidades;

    public PrediccionResponse(
            String clienteId,
            String planId,
            String tipo,
            String prediccion,
            String modelo,
            Map<String, Double> probabilidades
    ) {
        this.clienteId = clienteId;
        this.planId = planId;
        this.tipo = tipo;
        this.prediccion = prediccion;
        this.modelo = modelo;
        this.probabilidades = probabilidades;
    }

    public String getClienteId() {
        return clienteId;
    }

    public String getPlanId() {
        return planId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getPrediccion() {
        return prediccion;
    }

    public String getModelo() {
        return modelo;
    }

    public Map<String, Double> getProbabilidades() {
        return probabilidades;
    }
}