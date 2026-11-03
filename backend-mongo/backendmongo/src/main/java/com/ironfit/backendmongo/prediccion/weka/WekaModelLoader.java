package com.ironfit.backendmongo.prediccion.weka;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import weka.classifiers.Classifier;
import weka.core.SerializationHelper;

import java.io.InputStream;

@Component
public class WekaModelLoader {

    @Value("${ironfit.modelo.adherencia}")
    private Resource modeloAdherenciaResource;

    @Value("${ironfit.modelo.rendimiento}")
    private Resource modeloRendimientoResource;

    private Classifier modeloAdherencia;
    private Classifier modeloRendimiento;

    @PostConstruct
    public void cargarModelos() {
        try {
            try (InputStream inputAdherencia =
                         modeloAdherenciaResource.getInputStream()) {

                modeloAdherencia =
                        (Classifier) SerializationHelper.read(
                                inputAdherencia
                        );
            }

            try (InputStream inputRendimiento =
                         modeloRendimientoResource.getInputStream()) {

                modeloRendimiento =
                        (Classifier) SerializationHelper.read(
                                inputRendimiento
                        );
            }

            System.out.println(
                    "=========================================="
            );
            System.out.println(
                    "MODELOS WEKA CARGADOS CORRECTAMENTE"
            );
            System.out.println(
                    "Adherencia: "
                            + modeloAdherencia
                            .getClass()
                            .getSimpleName()
            );
            System.out.println(
                    "Rendimiento: "
                            + modeloRendimiento
                            .getClass()
                            .getSimpleName()
            );
            System.out.println(
                    "=========================================="
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "No fue posible cargar los modelos Weka",
                    e
            );
        }
    }

    public Classifier getModeloAdherencia() {
        return modeloAdherencia;
    }

    public Classifier getModeloRendimiento() {
        return modeloRendimiento;
    }

    public boolean modeloAdherenciaDisponible() {
        return modeloAdherencia != null;
    }

    public boolean modeloRendimientoDisponible() {
        return modeloRendimiento != null;
    }
}