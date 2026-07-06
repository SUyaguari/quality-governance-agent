package org.ups.citasalud.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ups.citasalud.application.usecase.BloquearFranjaUseCase;
import org.ups.citasalud.application.usecase.ConfirmarCitaUseCase;
import org.ups.citasalud.application.usecase.ConsultarFranjasDisponiblesUseCase;
import org.ups.citasalud.application.usecase.ListarMedicosUseCase;
import org.ups.citasalud.application.usecase.ListarMisCitasActivasUseCase;
import org.ups.citasalud.domain.port.CitaRepositoryPort;
import org.ups.citasalud.domain.port.FranjaHorariaRepositoryPort;
import org.ups.citasalud.domain.port.MedicoRepositoryPort;
import org.ups.citasalud.domain.port.NotificacionCitaPort;
import org.ups.citasalud.domain.port.PacienteAutenticadoPort;
import org.ups.citasalud.domain.port.RelojPort;

/**
 * Wiring de los Use Cases como beans de Spring (Principio I): los Use Cases en
 * {@code application.usecase} MUST permanecer libres de anotaciones de framework, igual que las
 * Entities. Este `@Configuration` es el único lugar de `application`/`domain` donde Spring
 * aparece — vive en `infrastructure/config`, no en la capa de aplicación.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public ConsultarFranjasDisponiblesUseCase consultarFranjasDisponiblesUseCase(
            FranjaHorariaRepositoryPort franjaHorariaRepositoryPort,
            MedicoRepositoryPort medicoRepositoryPort,
            RelojPort relojPort) {
        return new ConsultarFranjasDisponiblesUseCase(franjaHorariaRepositoryPort, medicoRepositoryPort, relojPort);
    }

    @Bean
    public ListarMedicosUseCase listarMedicosUseCase(MedicoRepositoryPort medicoRepositoryPort) {
        return new ListarMedicosUseCase(medicoRepositoryPort);
    }

    @Bean
    public BloquearFranjaUseCase bloquearFranjaUseCase(FranjaHorariaRepositoryPort franjaHorariaRepositoryPort,
                                                         RelojPort relojPort) {
        return new BloquearFranjaUseCase(franjaHorariaRepositoryPort, relojPort);
    }

    @Bean
    public ConfirmarCitaUseCase confirmarCitaUseCase(FranjaHorariaRepositoryPort franjaHorariaRepositoryPort,
                                                       CitaRepositoryPort citaRepositoryPort,
                                                       PacienteAutenticadoPort pacienteAutenticadoPort,
                                                       NotificacionCitaPort notificacionCitaPort,
                                                       RelojPort relojPort) {
        return new ConfirmarCitaUseCase(franjaHorariaRepositoryPort, citaRepositoryPort, pacienteAutenticadoPort,
                notificacionCitaPort, relojPort);
    }

    @Bean
    public ListarMisCitasActivasUseCase listarMisCitasActivasUseCase(CitaRepositoryPort citaRepositoryPort,
                                                                       PacienteAutenticadoPort pacienteAutenticadoPort,
                                                                       RelojPort relojPort) {
        return new ListarMisCitasActivasUseCase(citaRepositoryPort, pacienteAutenticadoPort, relojPort);
    }
}
