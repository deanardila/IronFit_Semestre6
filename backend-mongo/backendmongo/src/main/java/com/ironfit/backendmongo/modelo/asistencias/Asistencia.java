package com.ironfit.backendmongo.modelo.asistencias;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "asistencias")
public class Asistencia {

    @Id
    private String id;

    private String clienteId;
    private String planId;
    private String rutinaId;

    private LocalDate fechaAsistencia;

    private Boolean asistio;
    private Boolean cumplioRutina;

    private String observaciones;

    public Asistencia() {
    }

    public String getId() {
        return id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
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

    public LocalDate getFechaAsistencia() {
        return fechaAsistencia;
    }

    public void setFechaAsistencia(LocalDate fechaAsistencia) {
        this.fechaAsistencia = fechaAsistencia;
    }

    public Boolean getAsistio() {
        return asistio;
    }

    public void setAsistio(Boolean asistio) {
        this.asistio = asistio;
    }

    public Boolean getCumplioRutina() {
        return cumplioRutina;
    }

    public void setCumplioRutina(Boolean cumplioRutina) {
        this.cumplioRutina = cumplioRutina;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}