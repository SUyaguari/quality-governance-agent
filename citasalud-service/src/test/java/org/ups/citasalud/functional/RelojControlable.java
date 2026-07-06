package org.ups.citasalud.functional;

import org.ups.citasalud.domain.port.RelojPort;

import java.time.Duration;
import java.time.Instant;

/** Reloj de prueba (research.md §6) para simular el paso del tiempo sin esperar 5 minutos reales. */
public class RelojControlable implements RelojPort {

    private Instant instante = Instant.now();

    @Override
    public Instant ahora() {
        return instante;
    }

    public void avanzar(Duration duracion) {
        this.instante = this.instante.plus(duracion);
    }
}
