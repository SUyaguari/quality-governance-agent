package org.ups.citasalud.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.ups.citasalud.domain.model.Cita;
import org.ups.citasalud.infrastructure.persistence.model.CitaJpaEntity;
import org.ups.citasalud.infrastructure.persistence.model.MedicoJpaEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** BDD (Given-When-Then), integración real con H2. FR-018 (conteo/listado de citas activas). */
@DataJpaTest
@Import(CitaRepositoryAdapter.class)
@TestPropertySource(properties = "spring.sql.init.mode=never")
class CitaRepositoryAdapterTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private CitaRepositoryAdapter adapter;

    private final Instant ahora = Instant.parse("2026-08-01T10:00:00Z");

    @Test
    @DisplayName("Given una Cita nueva, When se guarda, Then queda persistida y recuperable")
    void given_citaNueva_when_guardar_then_persiste() {
        UUID medicoId = UUID.randomUUID();
        entityManager.persist(new MedicoJpaEntity(medicoId, "Dra. Ana Torres"));
        Cita cita = Cita.nueva(UUID.randomUUID(), UUID.randomUUID(), medicoId, UUID.randomUUID(), ahora.plusSeconds(3600));

        Cita guardada = adapter.guardar(cita);
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(CitaJpaEntity.class, guardada.getId())).isNotNull();
    }

    @Test
    @DisplayName("Given un paciente con 2 citas activas y 1 pasada, When se cuentan sus citas activas, Then retorna 2 (FR-018)")
    void given_pacienteConCitas_when_contarActivas_then_retornaSoloLasFuturas() {
        UUID medicoId = UUID.randomUUID();
        entityManager.persist(new MedicoJpaEntity(medicoId, "Dra. Ana Torres"));
        UUID pacienteId = UUID.randomUUID();

        adapter.guardar(Cita.nueva(UUID.randomUUID(), pacienteId, medicoId, UUID.randomUUID(), ahora.plusSeconds(3600)));
        adapter.guardar(Cita.nueva(UUID.randomUUID(), pacienteId, medicoId, UUID.randomUUID(), ahora.plusSeconds(7200)));
        adapter.guardar(Cita.nueva(UUID.randomUUID(), pacienteId, medicoId, UUID.randomUUID(), ahora.minusSeconds(3600))); // pasada
        entityManager.flush();

        int activas = adapter.contarActivasPorPaciente(pacienteId, ahora);

        assertThat(activas).isEqualTo(2);
    }

    @Test
    @DisplayName("Given citas de dos pacientes distintos, When se listan las activas de uno, Then no incluye las del otro")
    void given_citasDeDosPacientes_when_listarActivas_then_soloDeUnoDeEllos() {
        UUID medicoId = UUID.randomUUID();
        entityManager.persist(new MedicoJpaEntity(medicoId, "Dra. Ana Torres"));
        UUID pacienteA = UUID.randomUUID();
        UUID pacienteB = UUID.randomUUID();

        Cita citaA = adapter.guardar(Cita.nueva(UUID.randomUUID(), pacienteA, medicoId, UUID.randomUUID(), ahora.plusSeconds(3600)));
        adapter.guardar(Cita.nueva(UUID.randomUUID(), pacienteB, medicoId, UUID.randomUUID(), ahora.plusSeconds(3600)));
        entityManager.flush();

        List<Cita> activasDeA = adapter.listarActivasPorPaciente(pacienteA, ahora);

        assertThat(activasDeA).extracting(Cita::getId).containsExactly(citaA.getId());
    }
}
