package com.ironfit.backendmongo.controlador.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrivadoController {

    @GetMapping("/api/privado")
    public String privado() {
        return "Entraste con JWT correctamente";
    }
}