package com.ironfit.backendmongo.dto.asistencias;

public class AsistenciaResumenResponse {

    private long totalRegistros;
    private long totalAsistio;
    private long totalNoAsistio;
    private long totalSinRutina;
    private long totalParcial;
    private double porcentajeAsistencia;

    public AsistenciaResumenResponse() {
    }

    public AsistenciaResumenResponse(
            long totalRegistros,
            long totalAsistio,
            long totalNoAsistio,
            long totalSinRutina,
            long totalParcial,
            double porcentajeAsistencia
    ) {
        this.totalRegistros = totalRegistros;
        this.totalAsistio = totalAsistio;
        this.totalNoAsistio = totalNoAsistio;
        this.totalSinRutina = totalSinRutina;
        this.totalParcial = totalParcial;
        this.porcentajeAsistencia = porcentajeAsistencia;
    }

    public long getTotalRegistros() {
        return totalRegistros;
    }

    public void setTotalRegistros(long totalRegistros) {
        this.totalRegistros = totalRegistros;
    }

    public long getTotalAsistio() {
        return totalAsistio;
    }

    public void setTotalAsistio(long totalAsistio) {
        this.totalAsistio = totalAsistio;
    }

    public long getTotalNoAsistio() {
        return totalNoAsistio;
    }

    public void setTotalNoAsistio(long totalNoAsistio) {
        this.totalNoAsistio = totalNoAsistio;
    }

    public long getTotalSinRutina() {
        return totalSinRutina;
    }

    public void setTotalSinRutina(long totalSinRutina) {
        this.totalSinRutina = totalSinRutina;
    }

    public long getTotalParcial() {
        return totalParcial;
    }

    public void setTotalParcial(long totalParcial) {
        this.totalParcial = totalParcial;
    }

    public double getPorcentajeAsistencia() {
        return porcentajeAsistencia;
    }

    public void setPorcentajeAsistencia(double porcentajeAsistencia) {
        this.porcentajeAsistencia = porcentajeAsistencia;
    }
}
