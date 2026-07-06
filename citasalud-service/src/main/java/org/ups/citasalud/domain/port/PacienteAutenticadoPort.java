package org.ups.citasalud.domain.port;

import org.ups.citasalud.domain.model.PacienteAutenticado;

/**
 * FR-005/FR-006: expone la identidad y el perfil (nombre, documento, WhatsApp) del paciente que
 * tiene una sesión iniciada. La creación de la cuenta y el mecanismo de login en sí están fuera de
 * alcance de esta historia (ver plan.md, riesgo documentado).
 */
public interface PacienteAutenticadoPort {

    PacienteAutenticado obtenerActual();
}
