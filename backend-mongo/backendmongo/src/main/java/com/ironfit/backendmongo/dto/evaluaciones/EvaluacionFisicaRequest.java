package com.ironfit.backendmongo.dto.evaluaciones;

import java.time.LocalDateTime;

public class EvaluacionFisicaRequest {

    private String clienteId;
    private LocalDateTime fecha;

    private Double pesoCorporal;
    private Double talla;
    private Double porcentajeGraso;

    private Double pecho;
    private Double cintura;
    private Double brazo;
    private Double pierna;

    private String observaciones;

    public EvaluacionFisicaRequest() {
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
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

    public Double getPorcentajeGraso() {
        return porcentajeGraso;
    }

    public void setPorcentajeGraso(Double porcentajeGraso) {
        this.porcentajeGraso = porcentajeGraso;
    }

    public Double getPecho() {
        return pecho;
    }

    public void setPecho(Double pecho) {
        this.pecho = pecho;
    }

    public Double getCintura() {
        return cintura;
    }

    public void setCintura(Double cintura) {
        this.cintura = cintura;
    }

    public Double getBrazo() {
        return brazo;
    }

    public void setBrazo(Double brazo) {
        this.brazo = brazo;
    }

    public Double getPierna() {
        return pierna;
    }

    public void setPierna(Double pierna) {
        this.pierna = pierna;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}