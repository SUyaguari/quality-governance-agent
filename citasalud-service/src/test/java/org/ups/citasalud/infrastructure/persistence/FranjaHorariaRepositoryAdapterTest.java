package org.ups.citasalud.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.ups.citasalud.domain.model.EstadoFranja;
import org.ups.citasalud.infrastructure.persistence.model.FranjaHorariaJpaEntity;
import org.ups.citasalud.infrastructure.persistence.model.MedicoJpaEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BDD (Given-When-Then), integración real con H2. Prueba la escritura condicional atómica de
 * research.md §5 (FR-009/FR-011) y la restricción de unicidad (medicoId, fechaHora).
 */
@DataJpaTest
@Import(FranjaHorariaRepositoryAdapter.class)
@TestPropertySource(properties = "spring.sql.init.mode=never")
class FranjaHorariaRepositoryAdapterTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private FranjaHorariaRepositoryAdapter adapter;

    private final Instant ahora = Instant.parse("2026-08-01T10:00:00Z");

    private UUID crearMedico() {
        UUID medicoId = UUID.randomUUID();
        entityManager.persist(new MedicoJpaEntity(medicoId, "Dra. Ana Torres"));
        return medicoId;
    }

    private UUID crearFranjaDisponible(UUID medicoId, Instant fechaHora) {
        UUID franjaId = UUID.randomUUID();
        entityManager.persist(new FranjaHorariaJpaEntity(franjaId, medicoId, fechaHora, EstadoFranja.DISPONIBLE, null, null));
        entityManager.flush();
        return franjaId;
    }

    @Test
    @DisplayName("Given una franja disponible, When se bloquea, Then queda BLOQUEADA_TEMPORALMENTE en la base de datos (FR-009)")
    void given_franjaDisponible_when_bloquearSiDisponible_then_persisteBloqueo() {
        UUID medicoId = crearMedico();
        UUID franjaId = crearFranjaDisponible(medicoId, ahora.plusSeconds(3600));
        UUID pacienteId = UUID.randomUUID();

        boolean bloqueada = adapter.bloquearSiDisponible(franjaId, pacienteId, ahora, Duration.ofMinutes(5));
        entityManager.flush();
        entityManager.clear();

        assertThat(bloqueada).isTrue();
        FranjaHorariaJpaEntity persistida = entityManager.find(FranjaHorariaJpaEntity.class, franjaId);
        assertThat(persistida.getEstado()).isEqualTo(EstadoFranja.BLOQUEADA_TEMPORALMENTE);
        assertThat(persistida.getBloqueadaPorPacienteId()).isEqualTo(pacienteId);
    }

    @Test
    @DisplayName("Given una franja ya bloqueada vigente por otro paciente, When un segundo paciente intenta bloquearla, Then la escritura condicional falla (SC-003)")
    void given_franjaBloqueadaPorOtro_when_segundoPacienteBloquea_then_falla() {
        UUID medicoId = crearMedico();
        UUID franjaId = UUID.randomUUID();
        UUID pacienteA = UUID.randomUUID();
        UUID pacienteB = UUID.randomUUID();
        entityManager.persist(new FranjaHorariaJpaEntity(franjaId, medicoId, ahora.plusSeconds(3600),
                EstadoFranja.BLOQUEADA_TEMPORALMENTE, pacienteA, ahora.plusSeconds(60)));
        entityManager.flush();

        boolean bloqueadaPorB = adapter.bloquearSiDisponible(franjaId, pacienteB, ahora, Duration.ofMinutes(5));

        assertThat(bloqueadaPorB).isFalse();
    }

    @Test
    @DisplayName("Given una franja bloqueada a favor del paciente, When confirma antes de que expire, Then queda OCUPADA (FR-011)")
    void given_franjaBloqueadaAFavor_when_confirmar_then_quedaOcupada() {
        UUID medicoId = crearMedico();
        UUID franjaId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        entityManager.persist(new FranjaHorariaJpaEntity(franjaId, medicoId, ahora.plusSeconds(3600),
                EstadoFranja.BLOQUEADA_TEMPORALMENTE, pacienteId, ahora.plusSeconds(60)));
        entityManager.flush();

        boolean confirmada = adapter.confirmarSiBloqueadaPorPaciente(franjaId, pacienteId, ahora);
        entityManager.flush();
        entityManager.clear();

        assertThat(confirmada).isTrue();
        assertThat(entityManager.find(FranjaHorariaJpaEntity.class, franjaId).getEstado()).isEqualTo(EstadoFranja.OCUPADA);
    }

    @Test
    @DisplayName("Given una franja ya OCUPADA, When se intenta confirmar de nuevo (reintento), Then la escritura condicional falla (FR-012)")
    void given_franjaYaOcupada_when_seReintentaConfirmar_then_falla() {
        UUID medicoId = crearMedico();
        UUID franjaId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        entityManager.persist(new FranjaHorariaJpaEntity(franjaId, medicoId, ahora.plusSeconds(3600),
                EstadoFranja.OCUPADA, pacienteId, null));
        entityManager.flush();

        boolean confirmada = adapter.confirmarSiBloqueadaPorPaciente(franjaId, pacienteId, ahora);

        assertThat(confirmada).isFalse();
    }

    @Test
    @DisplayName("Given franjas pasadas, ocupadas y disponibles, When se listan las disponibles, Then solo retorna las futuras y libres (FR-001/FR-002/FR-004)")
    void given_variasFranjas_when_listarDisponibles_then_retornaSoloFuturasYLibres() {
        UUID medicoId = crearMedico();
        UUID franjaFuturaDisponible = crearFranjaDisponible(medicoId, ahora.plusSeconds(3600));
        entityManager.persist(new FranjaHorariaJpaEntity(UUID.randomUUID(), medicoId, ahora.minusSeconds(3600),
                EstadoFranja.DISPONIBLE, null, null)); // pasada
        entityManager.persist(new FranjaHorariaJpaEntity(UUID.randomUUID(), medicoId, ahora.plusSeconds(7200),
                EstadoFranja.OCUPADA, UUID.randomUUID(), null)); // ocupada
        entityManager.flush();
        entityManager.clear();

        List<org.ups.citasalud.domain.model.FranjaHoraria> disponibles = adapter.listarDisponibles(medicoId, ahora);

        assertThat(disponibles).hasSize(1);
        assertThat(disponibles.get(0).getId()).isEqualTo(franjaFuturaDisponible);
    }
}
