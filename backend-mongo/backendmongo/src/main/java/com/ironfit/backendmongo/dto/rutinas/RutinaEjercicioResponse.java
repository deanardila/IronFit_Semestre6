package com.ironfit.backendmongo.dto.rutinas;

public class RutinaEjercicioResponse {

    private String id;
    private String rutinaId;
    private String ejercicioId;

    private String ejercicioNombre;
    private String ejercicioDescripcion;
    private String categoria;
    private String grupoMuscular;
    private String tipoEquipo;

    private Integer series;
    private Integer repeticiones;
    private Integer descansoSegundos;
    private Integer tiempoSegundos;
    private Double pesoSugerido;
    private Integer orden;

    public RutinaEjercicioResponse() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getEjercicioNombre() {
        return ejercicioNombre;
    }

    public void setEjercicioNombre(String ejercicioNombre) {
        this.ejercicioNombre = ejercicioNombre;
    }

    public String getEjercicioDescripcion() {
        return ejercicioDescripcion;
    }

    public void setEjercicioDescripcion(String ejercicioDescripcion) {
        this.ejercicioDescripcion = ejercicioDescripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getGrupoMuscular() {
        return grupoMuscular;
    }

    public void setGrupoMuscular(String grupoMuscular) {
        this.grupoMuscular = grupoMuscular;
    }

    public String getTipoEquipo() {
        return tipoEquipo;
    }

    public void setTipoEquipo(String tipoEquipo) {
        this.tipoEquipo = tipoEquipo;
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