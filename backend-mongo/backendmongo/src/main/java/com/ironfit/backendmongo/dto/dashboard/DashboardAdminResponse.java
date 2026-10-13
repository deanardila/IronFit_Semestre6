package com.ironfit.backendmongo.dto.dashboard;

import java.util.List;

public class DashboardAdminResponse {

    private Long usuariosActivos;
    private Long clientesActivos;
    private Long entrenadoresActivos;
    private Long planesActivos;
    private Long asistenciasMes;
    private Double asistenciaGlobal;
    private Long evaluacionesRegistradas;
    private Long clientesSinPlan;

    private List<GraficoDatoResponse> usuariosPorRol;
    private List<GraficoDatoResponse> planesPorObjetivo;
    private List<GraficoDatoResponse> planesPorEntrenador;
    private List<GraficoDatoResponse> asistenciaUltimosDias;

    public Long getUsuariosActivos() {
        return usuariosActivos;
    }

    public void setUsuariosActivos(Long usuariosActivos) {
        this.usuariosActivos = usuariosActivos;
    }

    public Long getClientesActivos() {
        return clientesActivos;
    }

    public void setClientesActivos(Long clientesActivos) {
        this.clientesActivos = clientesActivos;
    }

    public Long getEntrenadoresActivos() {
        return entrenadoresActivos;
    }

    public void setEntrenadoresActivos(Long entrenadoresActivos) {
        this.entrenadoresActivos = entrenadoresActivos;
    }

    public Long getPlanesActivos() {
        return planesActivos;
    }

    public void setPlanesActivos(Long planesActivos) {
        this.planesActivos = planesActivos;
    }

    public Long getAsistenciasMes() {
        return asistenciasMes;
    }

    public void setAsistenciasMes(Long asistenciasMes) {
        this.asistenciasMes = asistenciasMes;
    }

    public Double getAsistenciaGlobal() {
        return asistenciaGlobal;
    }

    public void setAsistenciaGlobal(Double asistenciaGlobal) {
        this.asistenciaGlobal = asistenciaGlobal;
    }

    public Long getEvaluacionesRegistradas() {
        return evaluacionesRegistradas;
    }

    public void setEvaluacionesRegistradas(Long evaluacionesRegistradas) {
        this.evaluacionesRegistradas = evaluacionesRegistradas;
    }

    public Long getClientesSinPlan() {
        return clientesSinPlan;
    }

    public void setClientesSinPlan(Long clientesSinPlan) {
        this.clientesSinPlan = clientesSinPlan;
    }

    public List<GraficoDatoResponse> getUsuariosPorRol() {
        return usuariosPorRol;
    }

    public void setUsuariosPorRol(List<GraficoDatoResponse> usuariosPorRol) {
        this.usuariosPorRol = usuariosPorRol;
    }

    public List<GraficoDatoResponse> getPlanesPorObjetivo() {
        return planesPorObjetivo;
    }

    public void setPlanesPorObjetivo(List<GraficoDatoResponse> planesPorObjetivo) {
        this.planesPorObjetivo = planesPorObjetivo;
    }

    public List<GraficoDatoResponse> getPlanesPorEntrenador() {
        return planesPorEntrenador;
    }

    public void setPlanesPorEntrenador(List<GraficoDatoResponse> planesPorEntrenador) {
        this.planesPorEntrenador = planesPorEntrenador;
    }

    public List<GraficoDatoResponse> getAsistenciaUltimosDias() {
        return asistenciaUltimosDias;
    }

    public void setAsistenciaUltimosDias(List<GraficoDatoResponse> asistenciaUltimosDias) {
        this.asistenciaUltimosDias = asistenciaUltimosDias;
    }
}