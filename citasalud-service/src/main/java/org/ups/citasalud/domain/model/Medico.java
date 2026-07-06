package org.ups.citasalud.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Entidad de solo lectura para esta historia (data-model.md § Médico). */
public class Medico {

    private final UUID id;
    private final String nombre;

    public Medico(UUID id, String nombre) {
        this.id = Objects.requireNonNull(id);
        this.nombre = Objects.requireNonNull(nombre);
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
