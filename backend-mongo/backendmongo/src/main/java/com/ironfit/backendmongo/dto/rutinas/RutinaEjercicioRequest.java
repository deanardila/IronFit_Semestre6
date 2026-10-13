package com.ironfit.backendmongo.dto.rutinas;

public class RutinaEjercicioRequest {

    private String rutinaId;
    private String ejercicioId;

    private Integer series;
    private Integer repeticiones;
    private Integer descansoSegundos;
    private Integer tiempoSegundos;
    private Double pesoSugerido;
    private Integer orden;

    public RutinaEjercicioRequest() {
    }

    public String getRutinaId() {
        return rutinaId;
    }

    public void setRutinaId(String rutinaId) {
        this.rutinaId = rutinaId;
    }

    public String getEjercicioId() {
        return ejercicioId;
    }

    public void setEjercicioId(String ejercicioId) {
        this.ejercicioId = ejercicioId;
    }

    public Integer getSeries() {
        return series;
    }

    public void setSeries(Integer series) {
        this.series = series;
    }

    public Integer getRepeticiones() {
        return repeticiones;
    }

    public void setRepeticiones(Integer repeticiones) {
        this.repeticiones = repeticiones;
    }

    public Integer getDescansoSegundos() {
        return descansoSegundos;
    }

    public void setDescansoSegundos(Integer descansoSegundos) {
        this.descansoSegundos = descansoSegundos;
    }

    public Integer getTiempoSegundos() {
        return tiempoSegundos;
    }

    public void setTiempoSegundos(Integer tiempoSegundos) {
        this.tiempoSegundos = tiempoSegundos;
    }

    public Double getPesoSugerido() {
        return pesoSugerido;
    }

    public void setPesoSugerido(Double pesoSugerido) {
        this.pesoSugerido = pesoSugerido;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }
}