package com.ironfit.backendmongo.dto.asignaciones;

public class AsignacionEntrenadorClienteRequest {

    private String clienteId;
    private String entrenadorId;

    public AsignacionEntrenadorClienteRequest() {
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getEntrenadorId() {
        return entrenadorId;
    }

    public void setEntrenadorId(String entrenadorId) {
        this.entrenadorId = entrenadorId;
    }
}