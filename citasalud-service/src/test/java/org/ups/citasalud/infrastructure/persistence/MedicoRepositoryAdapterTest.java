package org.ups.citasalud.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.ups.citasalud.domain.model.Medico;
import org.ups.citasalud.infrastructure.persistence.model.MedicoJpaEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** BDD (Given-When-Then), integración real con H2. FR-001 (consulta de médicos, G1 de /speckit-analyze). */
@DataJpaTest
@Import(MedicoRepositoryAdapter.class)
@TestPropertySource(properties = "spring.sql.init.mode=never")
class MedicoRepositoryAdapterTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private MedicoRepositoryAdapter adapter;

    @Test
    @DisplayName("Given médicos persistidos, When se listan todos, Then retorna los médicos con su nombre (FR-001)")
    void given_medicosPersistidos_when_listarTodos_then_retornaTodos() {
        UUID medicoId = UUID.randomUUID();
        entityManager.persist(new MedicoJpaEntity(medicoId, "Dra. Ana Torres"));
        entityManager.flush();

        List<Medico> medicos = adapter.listarTodos();

        assertThat(medicos).extracting(Medico::getId).contains(medicoId);
        assertThat(medicos).extracting(Medico::getNombre).contains("Dra. Ana Torres");
    }

    @Test
    @DisplayName("Given un médico existente, When se consulta su existencia, Then retorna true; si no existe, retorna false")
    void given_medico_when_existe_then_true_o_false() {
        UUID medicoId = UUID.randomUUID();
        entityManager.persist(new MedicoJpaEntity(medicoId, "Dr. Luis Fernández"));
        entityManager.flush();

        assertThat(adapter.existe(medicoId)).isTrue();
        assertThat(adapter.existe(UUID.randomUUID())).isFalse();
    }
}
