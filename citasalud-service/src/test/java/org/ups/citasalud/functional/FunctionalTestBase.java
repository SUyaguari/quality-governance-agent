package org.ups.citasalud.functional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.UUID;

/**
 * Base común para las pruebas funcionales BDD (@SpringBootTest, Principio II) de los 6
 * Acceptance Scenarios de spec.md. Cada test crea sus propios médicos/franjas con UUIDs
 * aleatorios vía JdbcTemplate, para no depender de — ni interferir con — los datos de
 * db/data-sample.sql ni con los de otras clases de prueba que reutilicen el mismo contexto.
 */
/**
 * {@code @DirtiesContext} (AFTER_CLASS): cada clase de prueba funcional obtiene su propia base
 * H2 en memoria y su propio contexto Spring, para que el conteo de "citas activas" (FR-018) de
 * los pacientes de prueba fijos ({@link #PACIENTE_A}/{@link #PACIENTE_B}) no se contamine entre
 * clases de prueba distintas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class FunctionalTestBase {

    static final String PACIENTE_A = "paciente1";
    static final String PACIENTE_A_PASSWORD = "paciente1";
    static final String PACIENTE_B = "paciente2";
    static final String PACIENTE_B_PASSWORD = "paciente2";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    UUID crearMedico() {
        UUID medicoId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO medico (id, nombre) VALUES (?, ?)", medicoId, "Médico de prueba " + medicoId);
        return medicoId;
    }

    UUID crearFranjaDisponible(UUID medicoId, Instant fechaHora) {
        UUID franjaId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO franja_horaria (id, medico_id, fecha_hora, estado, bloqueada_por_paciente_id, bloqueada_hasta) "
                        + "VALUES (?, ?, ?, 'DISPONIBLE', NULL, NULL)",
                franjaId, medicoId, fechaHora);
        return franjaId;
    }

    TestRestTemplate como(String usuario, String password) {
        return restTemplate.withBasicAuth(usuario, password);
    }
}
