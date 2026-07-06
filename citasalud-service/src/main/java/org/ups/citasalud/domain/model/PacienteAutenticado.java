package org.ups.citasalud.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Datos del paciente actualmente autenticado, expuestos por {@link org.ups.citasalud.domain.port.PacienteAutenticadoPort}.
 * FR-006/FR-011/FR-014: nombreCompleto, documentoIdentificacion y numeroWhatsApp provienen del
 * perfil ya registrado del paciente (gestionado fuera de esta historia).
 */
public record PacienteAutenticado(UUID id, String nombreCompleto, String documentoIdentificacion, String numeroWhatsApp) {
    public PacienteAutenticado {
        Objects.requireNonNull(id);
        Objects.requireNonNull(nombreCompleto);
        Objects.requireNonNull(documentoIdentificacion);
        Objects.requireNonNull(numeroWhatsApp);
    }
}
