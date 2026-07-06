package org.ups.citasalud.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.ups.citasalud.application.usecase.ConfirmarCitaUseCase;
import org.ups.citasalud.application.usecase.ListarMisCitasActivasUseCase;
import org.ups.citasalud.infrastructure.web.generated.api.CitasApi;
import org.ups.citasalud.infrastructure.web.generated.model.Cita;
import org.ups.citasalud.infrastructure.web.generated.model.ConfirmarCitaRequest;

import java.util.List;

/** Implementa la interfaz generada por openapi-generator (Principio IV); no editar el generado. */
@RestController
public class CitasController implements CitasApi {

    private final ConfirmarCitaUseCase confirmarCitaUseCase;
    private final ListarMisCitasActivasUseCase listarMisCitasActivasUseCase;
    private final WebMapper mapper;

    public CitasController(ConfirmarCitaUseCase confirmarCitaUseCase,
                            ListarMisCitasActivasUseCase listarMisCitasActivasUseCase,
                            WebMapper mapper) {
        this.confirmarCitaUseCase = confirmarCitaUseCase;
        this.listarMisCitasActivasUseCase = listarMisCitasActivasUseCase;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<Cita> confirmarCita(ConfirmarCitaRequest confirmarCitaRequest) {
        org.ups.citasalud.domain.model.Cita cita = confirmarCitaUseCase.ejecutar(confirmarCitaRequest.getFranjaId());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.aDto(cita));
    }

    @Override
    public ResponseEntity<List<Cita>> listarMisCitasActivas() {
        List<Cita> citas = listarMisCitasActivasUseCase.ejecutar().stream()
                .map(mapper::aDto)
                .toList();
        return ResponseEntity.ok(citas);
    }
}
