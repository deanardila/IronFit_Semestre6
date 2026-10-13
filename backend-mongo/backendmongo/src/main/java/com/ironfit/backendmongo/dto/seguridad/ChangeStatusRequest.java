package com.ironfit.backendmongo.dto.seguridad;

public class ChangeStatusRequest {
    private boolean activo;

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
