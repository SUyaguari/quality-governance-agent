package org.ups.citasalud.domain.port;

import org.ups.citasalud.domain.model.Medico;

import java.util.List;
import java.util.UUID;

/** FR-001: consulta de solo lectura de médicos (ver G1 de /speckit-analyze). */
public interface MedicoRepositoryPort {

    List<Medico> listarTodos();

    boolean existe(UUID medicoId);
}
