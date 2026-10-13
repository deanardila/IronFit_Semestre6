package com.ironfit.backendmongo.dto.entrenamientos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class EntrenamientoRealizadoCrearRequest {

    @NotBlank(message = "El planId es obligatorio")
    private String planId;

    @NotBlank(message = "El rutinaId es obligatorio")
    private String rutinaId;

    private String rutinaNombre;

    @NotEmpty(message = "Debe registrar al menos un ejercicio realizado")
    @Valid
    private List<EjercicioRealizadoRequest> ejerciciosRealizados;

    @Size(max = 500, message = "La observación no puede superar 500 caracteres")
    private String observacionCliente;

    public EntrenamientoRealizadoCrearRequest() {
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getRutinaId() {
        return rutinaId;
    }

    public void setRutinaId(String rutinaId) {
        this.rutinaId = rutinaId;
    }

    public String getRutinaNombre() {
        return rutinaNombre;
    }

    public void setRutinaNombre(String rutinaNombre) {
        this.rutinaNombre = rutinaNombre;
    }

    public List<EjercicioRealizadoRequest> getEjerciciosRealizados() {
        return ejerciciosRealizados;
    }

    public void setEjerciciosRealizados(List<EjercicioRealizadoRequest> ejerciciosRealizados) {
        this.ejerciciosRealizados = ejerciciosRealizados;
    }

    public String getObservacionCliente() {
        return observacionCliente;
    }

    public void setObservacionCliente(String observacionCliente) {
        this.observacionCliente = observacionCliente;
    }
}