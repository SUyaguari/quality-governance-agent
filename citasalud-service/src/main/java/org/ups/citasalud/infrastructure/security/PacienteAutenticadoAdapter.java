package org.ups.citasalud.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.ups.citasalud.domain.model.PacienteAutenticado;
import org.ups.citasalud.domain.port.PacienteAutenticadoPort;

/** Adapter de {@link PacienteAutenticadoPort} sobre el contexto de seguridad de Spring (FR-005/FR-006). */
@Component
public class PacienteAutenticadoAdapter implements PacienteAutenticadoPort {

    @Override
    public PacienteAutenticado obtenerActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof PacienteUserDetails paciente)) {
            throw new IllegalStateException("No hay un paciente autenticado en el contexto de seguridad");
        }
        return new PacienteAutenticado(
                paciente.getPacienteId(),
                paciente.getNombreCompleto(),
                paciente.getDocumentoIdentificacion(),
                paciente.getNumeroWhatsApp());
    }
}
