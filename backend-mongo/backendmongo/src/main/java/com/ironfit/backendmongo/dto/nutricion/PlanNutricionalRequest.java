package com.ironfit.backendmongo.dto.nutricion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class PlanNutricionalRequest {

    @NotBlank(message = "El plan de entrenamiento es obligatorio")
    private String planId;

    @NotNull(message = "Las calorías objetivo son obligatorias")
    @Positive(message = "Las calorías objetivo deben ser mayores que cero")
    private Integer kcalObjetivo;

    @NotNull(message = "La proteína es obligatoria")
    @Positive(message = "La proteína debe ser mayor que cero")
    private Integer proteinaG;

    @NotNull(message = "Los carbohidratos son obligatorios")
    @Positive(message = "Los carbohidratos deben ser mayores que cero")
    private Integer carbohidratosG;

    @NotNull(message = "La grasa es obligatoria")
    @Positive(message = "La grasa debe ser mayor que cero")
    private Integer grasaG;

    @NotBlank(message = "La descripción del plan nutricional es obligatoria")
    @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
    private String descripcion;

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

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
}
