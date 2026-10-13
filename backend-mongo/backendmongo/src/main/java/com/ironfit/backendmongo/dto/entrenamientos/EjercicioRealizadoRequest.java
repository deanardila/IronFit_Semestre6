package com.ironfit.backendmongo.dto.entrenamientos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EjercicioRealizadoRequest {

    @NotBlank(message = "El ejercicioId es obligatorio")
    private String ejercicioId;

    private String ejercicioNombre;

    @NotNull(message = "Las series realizadas son obligatorias")
    @Min(value = 0, message = "Las series realizadas no pueden ser negativas")
    private Integer seriesRealizadas;

    @NotNull(message = "Las repeticiones realizadas son obligatorias")
    @Min(value = 0, message = "Las repeticiones realizadas no pueden ser negativas")
    private Integer repeticionesRealizadas;

    @Min(value = 0, message = "El peso usado no puede ser negativo")
    private Double pesoUsado;

    private Boolean completado = true;

    public EjercicioRealizadoRequest() {
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