package com.ironfit.backendmongo.dto.ejercicios;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EjercicioCrearRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 120)
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(min = 5, max = 500)
    private String descripcion;

    @NotBlank(message = "La categoría es obligatoria")
    private String categoria;

    @NotBlank(message = "El grupo muscular es obligatorio")
    private String grupoMuscular;

    @Min(1)
    @Max(20)
    private Integer seriesSugeridas;

    @Min(1)
    @Max(100)
    private Integer repeticionesSugeridas;

    private String tipoEquipo;

    // Getters y Setters

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