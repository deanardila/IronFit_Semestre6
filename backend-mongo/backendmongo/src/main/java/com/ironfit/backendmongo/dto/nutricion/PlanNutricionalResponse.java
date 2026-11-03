package com.ironfit.backendmongo.dto.nutricion;

import java.time.LocalDateTime;

public class PlanNutricionalResponse {

    private String id;
    private String planId;
    private String planNombre;
    private String clienteId;
    private String clienteNombre;
    private String entrenadorId;
    private String entrenadorNombre;
    private Integer kcalObjetivo;
    private Integer proteinaG;
    private Integer carbohidratosG;
    private Integer grasaG;
    private String descripcion;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getPlanNombre() { return planNombre; }
    public void setPlanNombre(String planNombre) { this.planNombre = planNombre; }

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getEntrenadorId() { return entrenadorId; }
    public void setEntrenadorId(String entrenadorId) { this.entrenadorId = entrenadorId; }

    public String getEntrenadorNombre() { return entrenadorNombre; }
    public void setEntrenadorNombre(String entrenadorNombre) { this.entrenadorNombre = entrenadorNombre; }

    public Integer getKcalObjetivo() { return kcalObjetivo; }
    public void setKcalObjetivo(Integer kcalObjetivo) { this.kcalObjetivo = kcalObjetivo; }

    public Integer getProteinaG() { return proteinaG; }
    public void setProteinaG(Integer proteinaG) { this.proteinaG = proteinaG; }

    public Integer getCarbohidratosG() { return carbohidratosG; }
    public void setCarbohidratosG(Integer carbohidratosG) { this.carbohidratosG = carbohidratosG; }

    public Integer getGrasaG() { return grasaG; }
    public void setGrasaG(Integer grasaG) { this.grasaG = grasaG; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
