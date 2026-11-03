package com.ironfit.backendmongo.prediccion.dto;

public class PrediccionPlanResponse {

    private String clienteId;
    private String planId;
    private PrediccionResponse adherencia;
    private PrediccionResponse rendimiento;

    public PrediccionPlanResponse(
            String clienteId,
            String planId,
            PrediccionResponse adherencia,
            PrediccionResponse rendimiento
    ) {
        this.clienteId = clienteId;
        this.planId = planId;
        this.adherencia = adherencia;
        this.rendimiento = rendimiento;
    }

    public String getClienteId() {
        return clienteId;
    }

    public String getPlanId() {
        return planId;
    }

    public PrediccionResponse getAdherencia() {
        return adherencia;
    }

    public PrediccionResponse getRendimiento() {
        return rendimiento;
    }
}