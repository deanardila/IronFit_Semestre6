package com.ironfit.backendmongo.dto.evaluaciones;

import com.ironfit.backendmongo.modelo.evaluaciones.MedidasCorporales;

import java.time.LocalDateTime;

public class EvaluacionFisicaResponse {

    private String id;

    private String clienteId;
    private String clienteNombre;

    private String entrenadorId;
    private String entrenadorNombre;

    private LocalDateTime fecha;

    private Double pesoCorporal;
    private Double talla;
    private Double imc;
    private Double porcentajeGraso;

    private MedidasCorporales medidasCorporales;

    private String observaciones;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public EvaluacionFisicaResponse() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getEntrenadorId() {
        return entrenadorId;
    }

    public void setEntrenadorId(String entrenadorId) {
        this.entrenadorId = entrenadorId;
    }

    public String getEntrenadorNombre() {
        return entrenadorNombre;
    }

    public void setEntrenadorNombre(String entrenadorNombre) {
        this.entrenadorNombre = entrenadorNombre;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Double getPesoCorporal() {
        return pesoCorporal;
    }

    public void setPesoCorporal(Double pesoCorporal) {
        this.pesoCorporal = pesoCorporal;
    }

    public Double getTalla() {
        return talla;
    }

    public void setTalla(Double talla) {
        this.talla = talla;
    }

    public Double getImc() {
        return imc;
    }

    public void setImc(Double imc) {
        this.imc = imc;
    }

    public Double getPorcentajeGraso() {
        return porcentajeGraso;
    }

    public void setPorcentajeGraso(Double porcentajeGraso) {
        this.porcentajeGraso = porcentajeGraso;
    }

    public MedidasCorporales getMedidasCorporales() {
        return medidasCorporales;
    }

    public void setMedidasCorporales(MedidasCorporales medidasCorporales) {
        this.medidasCorporales = medidasCorporales;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}