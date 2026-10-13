package com.ironfit.backendmongo.dto.entrenamientos;

public class EjercicioRealizadoResponse {

    private String ejercicioId;
    private String ejercicioNombre;
    private Integer seriesRealizadas;
    private Integer repeticionesRealizadas;
    private Double pesoUsado;
    private Boolean completado;

    public EjercicioRealizadoResponse() {
    }

    public String getEjercicioId() {
        return ejercicioId;
    }

    public void setEjercicioId(String ejercicioId) {
        this.ejercicioId = ejercicioId;
    }

    public String getEjercicioNombre() {
        return ejercicioNombre;
    }

    public void setEjercicioNombre(String ejercicioNombre) {
        this.ejercicioNombre = ejercicioNombre;
    }

    public Integer getSeriesRealizadas() {
        return seriesRealizadas;
    }

    public void setSeriesRealizadas(Integer seriesRealizadas) {
        this.seriesRealizadas = seriesRealizadas;
    }

    public Integer getRepeticionesRealizadas() {
        return repeticionesRealizadas;
    }

    public void setRepeticionesRealizadas(Integer repeticionesRealizadas) {
        this.repeticionesRealizadas = repeticionesRealizadas;
    }

    public Double getPesoUsado() {
        return pesoUsado;
    }

    public void setPesoUsado(Double pesoUsado) {
        this.pesoUsado = pesoUsado;
    }

    public Boolean getCompletado() {
        return completado;
    }

    public void setCompletado(Boolean completado) {
        this.completado = completado;
    }
}