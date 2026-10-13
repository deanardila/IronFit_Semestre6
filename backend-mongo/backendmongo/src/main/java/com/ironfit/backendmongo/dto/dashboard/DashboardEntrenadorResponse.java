package com.ironfit.backendmongo.dto.dashboard;

import java.util.List;

public class DashboardEntrenadorResponse {

    private Long clientesActivos;
    private Long planesActivos;
    private Long sesionesMes;
    private Double cumplimientoMes;
    private Long evaluacionesPendientes;
    private Long rutinasActivas;

    private List<GraficoDatoResponse> asistenciaUltimosDias;
    private List<GraficoDatoResponse> progresoPorCliente;
    private List<GraficoDatoResponse> planesPorEstado;
    private List<GraficoDatoResponse> evaluacionesPorCliente;

    public Long getClientesActivos() {
        return clientesActivos;
    }

    public void setClientesActivos(Long clientesActivos) {
        this.clientesActivos = clientesActivos;
    }

    public Long getPlanesActivos() {
        return planesActivos;
    }

    public void setPlanesActivos(Long planesActivos) {
        this.planesActivos = planesActivos;
    }

    public Long getSesionesMes() {
        return sesionesMes;
    }

    public void setSesionesMes(Long sesionesMes) {
        this.sesionesMes = sesionesMes;
    }

    public Double getCumplimientoMes() {
        return cumplimientoMes;
    }

    public void setCumplimientoMes(Double cumplimientoMes) {
        this.cumplimientoMes = cumplimientoMes;
    }

    public Long getEvaluacionesPendientes() {
        return evaluacionesPendientes;
    }

    public void setEvaluacionesPendientes(Long evaluacionesPendientes) {
        this.evaluacionesPendientes = evaluacionesPendientes;
    }

    public Long getRutinasActivas() {
        return rutinasActivas;
    }

    public void setRutinasActivas(Long rutinasActivas) {
        this.rutinasActivas = rutinasActivas;
    }

    public List<GraficoDatoResponse> getAsistenciaUltimosDias() {
        return asistenciaUltimosDias;
    }

    public void setAsistenciaUltimosDias(List<GraficoDatoResponse> asistenciaUltimosDias) {
        this.asistenciaUltimosDias = asistenciaUltimosDias;
    }

    public List<GraficoDatoResponse> getProgresoPorCliente() {
        return progresoPorCliente;
    }

    public void setProgresoPorCliente(List<GraficoDatoResponse> progresoPorCliente) {
        this.progresoPorCliente = progresoPorCliente;
    }

    public List<GraficoDatoResponse> getPlanesPorEstado() {
        return planesPorEstado;
    }

    public void setPlanesPorEstado(List<GraficoDatoResponse> planesPorEstado) {
        this.planesPorEstado = planesPorEstado;
    }

    public List<GraficoDatoResponse> getEvaluacionesPorCliente() {
        return evaluacionesPorCliente;
    }

    public void setEvaluacionesPorCliente(List<GraficoDatoResponse> evaluacionesPorCliente) {
        this.evaluacionesPorCliente = evaluacionesPorCliente;
    }
}