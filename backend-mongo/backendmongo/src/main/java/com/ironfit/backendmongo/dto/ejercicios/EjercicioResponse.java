package com.ironfit.backendmongo.dto.ejercicios;

public class EjercicioResponse {

    private String id;
    private String nombre;
    private String descripcion;
    private String categoria;
    private String grupoMuscular;
    private Integer seriesSugeridas;
    private Integer repeticionesSugeridas;
    private String tipoEquipo;

    public EjercicioResponse() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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

    public Integer getSeriesSugeridas() {
        return seriesSugeridas;
    }

    public void setSeriesSugeridas(Integer seriesSugeridas) {
        this.seriesSugeridas = seriesSugeridas;
    }

    public Integer getRepeticionesSugeridas() {
        return repeticionesSugeridas;
    }

    public void setRepeticionesSugeridas(Integer repeticionesSugeridas) {
        this.repeticionesSugeridas = repeticionesSugeridas;
    }

    public String getTipoEquipo() {
        return tipoEquipo;
    }

    public void setTipoEquipo(String tipoEquipo) {
        this.tipoEquipo = tipoEquipo;
    }
}