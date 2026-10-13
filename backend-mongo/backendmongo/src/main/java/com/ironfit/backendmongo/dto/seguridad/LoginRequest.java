package com.ironfit.backendmongo.dto.seguridad;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank
    private String correo;

    @NotBlank
    private String password;

    public String getCorreo() { return correo; }
    public String getPassword() { return password; }
    
    public void setCorreo(String correo) { this.correo = correo; }
    public void setPassword(String password) { this.password = password; }
}