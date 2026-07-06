package org.ups.citasalud.infrastructure.config;

import org.springframework.stereotype.Component;
import org.ups.citasalud.domain.port.RelojPort;

import java.time.Instant;

/** Implementación real de {@link RelojPort} sobre el reloj del sistema. */
@Component
public class RelojSistema implements RelojPort {

    @Override
    public Instant ahora() {
        return Instant.now();
    }
}
