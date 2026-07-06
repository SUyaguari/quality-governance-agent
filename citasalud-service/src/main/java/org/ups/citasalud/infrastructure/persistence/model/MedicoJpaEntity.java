package org.ups.citasalud.infrastructure.persistence.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ups.citasalud.domain.model.Medico;

import java.util.UUID;

/** Entidad JPA de solo lectura (data-model.md § Médico). Distinta del modelo de dominio (Principio I). */
@Entity
@Table(name = "medico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedicoJpaEntity {

    @Id
    private UUID id;

    private String nombre;

    public Medico toDomain() {
        return new Medico(id, nombre);
    }
}
