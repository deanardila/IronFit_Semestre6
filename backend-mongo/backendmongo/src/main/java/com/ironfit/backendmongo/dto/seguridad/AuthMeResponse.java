package com.ironfit.backendmongo.dto.seguridad;

import java.util.List;

public class AuthMeResponse {

    private String id;
    private String correo;
    private List<String> roles;
    private String nombres;
    private String apellidos;

    public AuthMeResponse() {
    }

    public AuthMeResponse(String id, String correo, List<String> roles, String nombres, String apellidos) {
        this.id = id;
        this.correo = correo;
        this.roles = roles;
        this.nombres = nombres;
        this.apellidos = apellidos;
    }

    public String getId() {
        return id;
    }

    public String getCorreo() {
        return correo;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }
}