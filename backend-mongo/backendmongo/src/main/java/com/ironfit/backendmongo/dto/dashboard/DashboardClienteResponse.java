package com.ironfit.backendmongo.dto.dashboard;

import java.util.List;

public class DashboardClienteResponse {

    private String planActual;
    private Long rutinasActivas;
    private Long sesionesMes;
    private Long rachaActual;
    private Double progresoPlan;
    private Double pesoActual;
    private Double imcActual;

    private List<GraficoDatoResponse> asistenciaUltimosDias;
    private List<GraficoDatoResponse> evolucionPeso;
    private List<GraficoDatoResponse> metricasCorporales;
    private List<GraficoDatoResponse> progresoPlanGrafico;

    public String getPlanActual() {
        return planActual;
    }

    public void setPlanActual(String planActual) {
        this.planActual = planActual;
    }

    public Long getRutinasActivas() {
        return rutinasActivas;
    }

    public void setRutinasActivas(Long rutinasActivas) {
        this.rutinasActivas = rutinasActivas;
    }

    public Long getSesionesMes() {
        return sesionesMes;
    }

    public void setSesionesMes(Long sesionesMes) {
        this.sesionesMes = sesionesMes;
    }

    public Long getRachaActual() {
        return rachaActual;
    }

    public void setRachaActual(Long rachaActual) {
        this.rachaActual = rachaActual;
    }

    public Double getProgresoPlan() {
        return progresoPlan;
    }

    public void setProgresoPlan(Double progresoPlan) {
        this.progresoPlan = progresoPlan;
    }

    public Double getPesoActual() {
        return pesoActual;
    }

    public void setPesoActual(Double pesoActual) {
        this.pesoActual = pesoActual;
    }

    public Double getImcActual() {
        return imcActual;
    }

    public void setImcActual(Double imcActual) {
        this.imcActual = imcActual;
    }

    public List<GraficoDatoResponse> getAsistenciaUltimosDias() {
        return asistenciaUltimosDias;
    }

    public void setAsistenciaUltimosDias(List<GraficoDatoResponse> asistenciaUltimosDias) {
        this.asistenciaUltimosDias = asistenciaUltimosDias;
    }

    public List<GraficoDatoResponse> getEvolucionPeso() {
        return evolucionPeso;
    }

    public void setEvolucionPeso(List<GraficoDatoResponse> evolucionPeso) {
        this.evolucionPeso = evolucionPeso;
    }

    public List<GraficoDatoResponse> getMetricasCorporales() {
        return metricasCorporales;
    }

    public void setMetricasCorporales(List<GraficoDatoResponse> metricasCorporales) {
        this.metricasCorporales = metricasCorporales;
    }

    public List<GraficoDatoResponse> getProgresoPlanGrafico() {
        return progresoPlanGrafico;
    }

    public void setProgresoPlanGrafico(List<GraficoDatoResponse> progresoPlanGrafico) {
        this.progresoPlanGrafico = progresoPlanGrafico;
    }
}