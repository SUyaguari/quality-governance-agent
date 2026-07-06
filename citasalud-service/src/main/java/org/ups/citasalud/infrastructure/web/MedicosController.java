package org.ups.citasalud.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.ups.citasalud.application.usecase.ConsultarFranjasDisponiblesUseCase;
import org.ups.citasalud.application.usecase.ListarMedicosUseCase;
import org.ups.citasalud.infrastructure.web.generated.api.MedicosApi;
import org.ups.citasalud.infrastructure.web.generated.model.FranjaHoraria;
import org.ups.citasalud.infrastructure.web.generated.model.Medico;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Implementa la interfaz generada por openapi-generator (Principio IV); no editar el generado. */
@RestController
public class MedicosController implements MedicosApi {

    private final ListarMedicosUseCase listarMedicosUseCase;
    private final ConsultarFranjasDisponiblesUseCase consultarFranjasDisponiblesUseCase;
    private final WebMapper mapper;

    public MedicosController(ListarMedicosUseCase listarMedicosUseCase,
                              ConsultarFranjasDisponiblesUseCase consultarFranjasDisponiblesUseCase,
                              WebMapper mapper) {
        this.listarMedicosUseCase = listarMedicosUseCase;
        this.consultarFranjasDisponiblesUseCase = consultarFranjasDisponiblesUseCase;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<List<Medico>> listarMedicos() {
        List<Medico> medicos = listarMedicosUseCase.ejecutar().stream()
                .map(mapper::aDto)
                .toList();
        return ResponseEntity.ok(medicos);
    }

    @Override
    public ResponseEntity<List<FranjaHoraria>> listarFranjasDisponibles(UUID medicoId, OffsetDateTime desde, OffsetDateTime hasta) {
        List<FranjaHoraria> franjas = consultarFranjasDisponiblesUseCase.ejecutar(medicoId).stream()
                .map(mapper::aDto)
                .toList();
        return ResponseEntity.ok(franjas);
    }
}
