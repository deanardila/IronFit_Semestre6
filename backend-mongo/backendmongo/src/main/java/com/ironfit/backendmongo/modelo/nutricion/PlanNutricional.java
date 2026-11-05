package com.ironfit.backendmongo.modelo.nutricion;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "planes_nutricionales")
public class PlanNutricional {

    @Id
    private String id;

    @Indexed(unique = true)
    private String planId;

    @Indexed
    private String clienteId;

    @Indexed
    private String entrenadorId;

    private Integer kcalObjetivo;
    private Integer proteinaG;
    private Integer carbohidratosG;
    private Integer grasaG;
    private String descripcion;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public PlanNutricional() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public String getEntrenadorId() { return entrenadorId; }
    public void setEntrenadorId(String entrenadorId) { this.entrenadorId = entrenadorId; }

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
