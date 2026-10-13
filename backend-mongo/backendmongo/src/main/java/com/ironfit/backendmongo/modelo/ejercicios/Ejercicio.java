package com.ironfit.backendmongo.modelo.ejercicios;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "ejercicios")
public class Ejercicio {

    @Id
    private String id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(min = 5, max = 500, message = "La descripción debe tener entre 5 y 500 caracteres")
    private String descripcion;

    @NotBlank(message = "La categoría es obligatoria")
    @Size(max = 80, message = "La categoría no puede superar 80 caracteres")
    private String categoria;

    @NotBlank(message = "El grupo muscular es obligatorio")
    @Size(max = 80, message = "El grupo muscular no puede superar 80 caracteres")
    private String grupoMuscular;

    @Min(value = 1, message = "Las series sugeridas deben ser mínimo 1")
    @Max(value = 20, message = "Las series sugeridas deben ser máximo 20")
    private Integer seriesSugeridas;

    @Min(value = 1, message = "Las repeticiones sugeridas deben ser mínimo 1")
    @Max(value = 100, message = "Las repeticiones sugeridas deben ser máximo 100")
    private Integer repeticionesSugeridas;

    @Size(max = 80, message = "El tipo de equipo no puede superar 80 caracteres")
    private String tipoEquipo;

    private Boolean activo = true;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public Ejercicio() {
    }

    public Ejercicio(String id, String nombre, String descripcion, String categoria, String grupoMuscular,
                        Integer seriesSugeridas, Integer repeticionesSugeridas, String tipoEquipo,
                        Boolean activo, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.grupoMuscular = grupoMuscular;
        this.seriesSugeridas = seriesSugeridas;
        this.repeticionesSugeridas = repeticionesSugeridas;
        this.tipoEquipo = tipoEquipo;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
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

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
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