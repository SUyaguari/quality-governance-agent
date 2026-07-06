package org.ups.citasalud.domain.port;

import java.time.Instant;

/** Abstracción de reloj (research.md §6) para poder simular el paso del tiempo en pruebas (FR-010). */
public interface RelojPort {

    Instant ahora();
}
