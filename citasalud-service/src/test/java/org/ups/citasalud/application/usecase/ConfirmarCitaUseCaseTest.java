package org.ups.citasalud.application.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ups.citasalud.domain.exception.FranjaNoDisponibleException;
import org.ups.citasalud.domain.exception.FranjaNoEncontradaException;
import org.ups.citasalud.domain.exception.LimiteCitasActivasExcedidoException;
import org.ups.citasalud.domain.model.Cita;
import org.ups.citasalud.domain.model.EstadoNotificacion;
import org.ups.citasalud.domain.model.FranjaHoraria;
import org.ups.citasalud.domain.model.PacienteAutenticado;
import org.ups.citasalud.domain.port.CitaRepositoryPort;
import org.ups.citasalud.domain.port.FranjaHorariaRepositoryPort;
import org.ups.citasalud.domain.port.NotificacionCitaPort;
import org.ups.citasalud.domain.port.PacienteAutenticadoPort;
import org.ups.citasalud.domain.port.RelojPort;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BDD (Given-When-Then), Ports doblados. FR-006/FR-008/FR-011/FR-012/FR-013/FR-017/FR-018.
 */
@ExtendWith(MockitoExtension.class)
class ConfirmarCitaUseCaseTest {

    @Mock
    private FranjaHorariaRepositoryPort franjaHorariaRepositoryPort;
    @Mock
    private CitaRepositoryPort citaRepositoryPort;
    @Mock
    private PacienteAutenticadoPort pacienteAutenticadoPort;
    @Mock
    private NotificacionCitaPort notificacionCitaPort;
    @Mock
    private RelojPort relojPort;

    private final Instant ahora = Instant.parse("2026-08-01T10:00:00Z");
    private final UUID franjaId = UUID.randomUUID();
    private final PacienteAutenticado paciente = new PacienteAutenticado(
            UUID.randomUUID(), "María Pérez", "0102030405", "+593987654321");

    private ConfirmarCitaUseCase crearUseCase() {
        return new ConfirmarCitaUseCase(franjaHorariaRepositoryPort, citaRepositoryPort,
                pacienteAutenticadoPort, notificacionCitaPort, relojPort);
    }

    private FranjaHoraria franjaBloqueadaParaPaciente() {
        return new FranjaHoraria(franjaId, UUID.randomUUID(), ahora.plusSeconds(3600),
                org.ups.citasalud.domain.model.EstadoFranja.BLOQUEADA_TEMPORALMENTE, paciente.id(), ahora.plusSeconds(60));
    }

    @Nested
    @DisplayName("FR-006/FR-008/FR-011: confirmación exitosa")
    class ConfirmacionExitosa {

        @Test
        @DisplayName("Given franja bloqueada a favor del paciente y menos de 2 citas activas, When confirma, Then registra la cita usando el WhatsApp del perfil")
        void given_franjaBloqueadaYSinLimite_when_ejecutar_then_registraCita() {
            when(pacienteAutenticadoPort.obtenerActual()).thenReturn(paciente);
            when(relojPort.ahora()).thenReturn(ahora);
            when(franjaHorariaRepositoryPort.buscarPorId(franjaId)).thenReturn(Optional.of(franjaBloqueadaParaPaciente()));
            when(citaRepositoryPort.contarActivasPorPaciente(paciente.id(), ahora)).thenReturn(0);
            when(franjaHorariaRepositoryPort.confirmarSiBloqueadaPorPaciente(franjaId, paciente.id(), ahora)).thenReturn(true);
            when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

            Cita resultado = crearUseCase().ejecutar(franjaId);

            assertThat(resultado.getPacienteId()).isEqualTo(paciente.id());
            assertThat(resultado.getFranjaHorariaId()).isEqualTo(franjaId);
            verify(notificacionCitaPort).enviarConfirmacion(any(Cita.class), eq(paciente.nombreCompleto()), eq(paciente.numeroWhatsApp()));
        }

        @Test
        @DisplayName("Given que el envío de WhatsApp falla, When confirma, Then la cita queda registrada igual con estadoNotificacion FALLIDA (FR-017)")
        void given_envioFalla_when_ejecutar_then_citaQuedaRegistradaComoFallida() {
            when(pacienteAutenticadoPort.obtenerActual()).thenReturn(paciente);
            when(relojPort.ahora()).thenReturn(ahora);
            when(franjaHorariaRepositoryPort.buscarPorId(franjaId)).thenReturn(Optional.of(franjaBloqueadaParaPaciente()));
            when(citaRepositoryPort.contarActivasPorPaciente(paciente.id(), ahora)).thenReturn(0);
            when(franjaHorariaRepositoryPort.confirmarSiBloqueadaPorPaciente(franjaId, paciente.id(), ahora)).thenReturn(true);
            when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new NotificacionCitaPort.NotificacionFallidaException("falló", new RuntimeException()))
                    .when(notificacionCitaPort).enviarConfirmacion(any(Cita.class), any(), any());

            Cita resultado = crearUseCase().ejecutar(franjaId);

            assertThat(resultado.getEstadoNotificacion()).isEqualTo(EstadoNotificacion.FALLIDA);
            ArgumentCaptor<Cita> captor = ArgumentCaptor.forClass(Cita.class);
            verify(citaRepositoryPort, times(2)).guardar(captor.capture());
        }
    }

    @Test
    @DisplayName("Given una franja inexistente, When se intenta confirmar, Then lanza FranjaNoEncontradaException")
    void given_franjaInexistente_when_ejecutar_then_lanzaFranjaNoEncontrada() {
        when(pacienteAutenticadoPort.obtenerActual()).thenReturn(paciente);
        when(relojPort.ahora()).thenReturn(ahora);
        when(franjaHorariaRepositoryPort.buscarPorId(franjaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> crearUseCase().ejecutar(franjaId)).isInstanceOf(FranjaNoEncontradaException.class);
    }

    @Test
    @DisplayName("Given el bloqueo ya expiró, When se intenta confirmar, Then lanza FranjaNoDisponibleException (FR-013)")
    void given_bloqueoExpirado_when_ejecutar_then_lanzaFranjaNoDisponible() {
        when(pacienteAutenticadoPort.obtenerActual()).thenReturn(paciente);
        when(relojPort.ahora()).thenReturn(ahora);
        when(franjaHorariaRepositoryPort.buscarPorId(franjaId)).thenReturn(Optional.of(franjaBloqueadaParaPaciente()));
        when(citaRepositoryPort.contarActivasPorPaciente(paciente.id(), ahora)).thenReturn(0);
        when(franjaHorariaRepositoryPort.confirmarSiBloqueadaPorPaciente(franjaId, paciente.id(), ahora)).thenReturn(false);

        assertThatThrownBy(() -> crearUseCase().ejecutar(franjaId)).isInstanceOf(FranjaNoDisponibleException.class);
        verify(citaRepositoryPort, never()).guardar(any());
    }

    @Test
    @DisplayName("Given una solicitud de confirmación duplicada sobre la misma franja ya confirmada, When se reintenta, Then la segunda llamada falla sin registrar una segunda cita (FR-012)")
    void given_solicitudDuplicada_when_seReintentaConfirmar_then_fallaSinDuplicar() {
        when(pacienteAutenticadoPort.obtenerActual()).thenReturn(paciente);
        when(relojPort.ahora()).thenReturn(ahora);
        when(franjaHorariaRepositoryPort.buscarPorId(franjaId)).thenReturn(Optional.of(franjaBloqueadaParaPaciente()));
        when(citaRepositoryPort.contarActivasPorPaciente(paciente.id(), ahora)).thenReturn(0);
        // Primera llamada: éxito. Segunda llamada (reintento del mismo request): la franja ya
        // está OCUPADA, por lo que la escritura condicional atómica falla (research.md §5).
        when(franjaHorariaRepositoryPort.confirmarSiBloqueadaPorPaciente(franjaId, paciente.id(), ahora))
                .thenReturn(true)
                .thenReturn(false);
        when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfirmarCitaUseCase useCase = crearUseCase();
        Cita primeraCita = useCase.ejecutar(franjaId);

        assertThatThrownBy(() -> useCase.ejecutar(franjaId)).isInstanceOf(FranjaNoDisponibleException.class);
        assertThat(primeraCita).isNotNull();
        verify(citaRepositoryPort, times(2)).guardar(any(Cita.class)); // 1 registro + 1 update de notificación
    }

    @Test
    @DisplayName("Given el paciente ya tiene 2 citas activas, When intenta confirmar una nueva, Then lanza LimiteCitasActivasExcedidoException (FR-018)")
    void given_limiteDeCitasAlcanzado_when_ejecutar_then_lanzaLimiteExcedido() {
        when(pacienteAutenticadoPort.obtenerActual()).thenReturn(paciente);
        when(relojPort.ahora()).thenReturn(ahora);
        when(franjaHorariaRepositoryPort.buscarPorId(franjaId)).thenReturn(Optional.of(franjaBloqueadaParaPaciente()));
        when(citaRepositoryPort.contarActivasPorPaciente(paciente.id(), ahora))
                .thenReturn(ConfirmarCitaUseCase.LIMITE_CITAS_ACTIVAS);

        assertThatThrownBy(() -> crearUseCase().ejecutar(franjaId)).isInstanceOf(LimiteCitasActivasExcedidoException.class);
        verify(franjaHorariaRepositoryPort, never()).confirmarSiBloqueadaPorPaciente(any(), any(), any());
    }
}
