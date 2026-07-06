package org.ups.citasalud.infrastructure.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.ups.citasalud.application.usecase.BloquearFranjaUseCase;
import org.ups.citasalud.application.usecase.ConfirmarCitaUseCase;
import org.ups.citasalud.application.usecase.ConsultarFranjasDisponiblesUseCase;
import org.ups.citasalud.application.usecase.ListarMedicosUseCase;
import org.ups.citasalud.application.usecase.ListarMisCitasActivasUseCase;
import org.ups.citasalud.domain.exception.FranjaNoDisponibleException;
import org.ups.citasalud.domain.exception.FranjaNoEncontradaException;
import org.ups.citasalud.domain.exception.MedicoNoEncontradoException;
import org.ups.citasalud.domain.model.Medico;
import org.ups.citasalud.domain.port.PacienteAutenticadoPort;
import org.ups.citasalud.infrastructure.security.SecurityConfig;
import org.ups.citasalud.infrastructure.security.SesionNoIniciadaEntryPoint;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BDD (Given-When-Then), integración MockMvc contra los DTOs generados. FR-001, FR-004,
 * FR-005/SC-006.
 */
@WebMvcTest(controllers = {MedicosController.class, FranjasController.class, CitasController.class,
        GlobalExceptionHandler.class})
@Import({SecurityConfig.class, SesionNoIniciadaEntryPoint.class, WebMapper.class})
class CitasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarMedicosUseCase listarMedicosUseCase;
    @MockitoBean
    private ConsultarFranjasDisponiblesUseCase consultarFranjasDisponiblesUseCase;
    @MockitoBean
    private BloquearFranjaUseCase bloquearFranjaUseCase;
    @MockitoBean
    private ConfirmarCitaUseCase confirmarCitaUseCase;
    @MockitoBean
    private ListarMisCitasActivasUseCase listarMisCitasActivasUseCase;
    @MockitoBean
    private PacienteAutenticadoPort pacienteAutenticadoPort;

    @Test
    @DisplayName("Given una persona sin sesión iniciada, When consulta GET /medicos, Then responde 401 con code NO_AUTENTICADO (FR-005/SC-006)")
    void given_sinSesion_when_getMedicos_then_401() throws Exception {
        mockMvc.perform(get("/medicos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test
    @WithMockUser
    @DisplayName("Given un paciente autenticado, When consulta GET /medicos, Then responde 200 con la lista de médicos (FR-001)")
    void given_pacienteAutenticado_when_getMedicos_then_200() throws Exception {
        UUID medicoId = UUID.randomUUID();
        when(listarMedicosUseCase.ejecutar()).thenReturn(List.of(new Medico(medicoId, "Dra. Ana Torres")));

        mockMvc.perform(get("/medicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Dra. Ana Torres"));
    }

    @Test
    @WithMockUser
    @DisplayName("Given un médico inexistente, When consulta sus franjas, Then responde 404 con code MEDICO_NO_ENCONTRADO")
    void given_medicoInexistente_when_getFranjas_then_404() throws Exception {
        UUID medicoId = UUID.randomUUID();
        when(consultarFranjasDisponiblesUseCase.ejecutar(medicoId))
                .thenThrow(new MedicoNoEncontradoException("No existe"));

        mockMvc.perform(get("/medicos/{medicoId}/franjas", medicoId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEDICO_NO_ENCONTRADO"));
    }

    @Test
    @WithMockUser
    @DisplayName("Given una franja ya no disponible, When se intenta bloquear, Then responde 409 con code FRANJA_NO_DISPONIBLE (FR-004)")
    void given_franjaNoDisponible_when_bloquear_then_409() throws Exception {
        UUID franjaId = UUID.randomUUID();
        when(pacienteAutenticadoPort.obtenerActual()).thenReturn(
                new org.ups.citasalud.domain.model.PacienteAutenticado(UUID.randomUUID(), "María Pérez", "0102030405", "+593987654321"));
        when(bloquearFranjaUseCase.ejecutar(org.mockito.ArgumentMatchers.eq(franjaId), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new FranjaNoDisponibleException("no disponible"));

        mockMvc.perform(post("/franjas/{franjaId}/bloqueos", franjaId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FRANJA_NO_DISPONIBLE"));
    }

    @Test
    @WithMockUser
    @DisplayName("Given una franja inexistente, When se intenta bloquear, Then responde 404 con code FRANJA_NO_ENCONTRADA")
    void given_franjaInexistente_when_bloquear_then_404() throws Exception {
        UUID franjaId = UUID.randomUUID();
        when(pacienteAutenticadoPort.obtenerActual()).thenReturn(
                new org.ups.citasalud.domain.model.PacienteAutenticado(UUID.randomUUID(), "María Pérez", "0102030405", "+593987654321"));
        when(bloquearFranjaUseCase.ejecutar(org.mockito.ArgumentMatchers.eq(franjaId), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new FranjaNoEncontradaException("no existe"));

        mockMvc.perform(post("/franjas/{franjaId}/bloqueos", franjaId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FRANJA_NO_ENCONTRADA"));
    }
}
