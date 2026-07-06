package org.ups.citasalud.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.ups.citasalud.application.usecase.BloquearFranjaUseCase;
import org.ups.citasalud.domain.port.PacienteAutenticadoPort;
import org.ups.citasalud.infrastructure.web.generated.api.FranjasApi;
import org.ups.citasalud.infrastructure.web.generated.model.BloqueoTemporal;

import java.time.Instant;
import java.util.UUID;

/** Implementa la interfaz generada por openapi-generator (Principio IV); no editar el generado. */
@RestController
public class FranjasController implements FranjasApi {

    private final BloquearFranjaUseCase bloquearFranjaUseCase;
    private final PacienteAutenticadoPort pacienteAutenticadoPort;
    private final WebMapper mapper;

    public FranjasController(BloquearFranjaUseCase bloquearFranjaUseCase,
                              PacienteAutenticadoPort pacienteAutenticadoPort,
                              WebMapper mapper) {
        this.bloquearFranjaUseCase = bloquearFranjaUseCase;
        this.pacienteAutenticadoPort = pacienteAutenticadoPort;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<BloqueoTemporal> bloquearFranjaTemporalmente(UUID franjaId) {
        UUID pacienteId = pacienteAutenticadoPort.obtenerActual().id();
        Instant bloqueadaHasta = bloquearFranjaUseCase.ejecutar(franjaId, pacienteId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.aBloqueoTemporalDto(franjaId, bloqueadaHasta));
    }
}
