# Data Model: Reserva de cita en línea 24/7

**Feature**: `001-reserva-cita-online-24-7` · **Date**: 2026-07-02

Derivado de la sección "Key Entities" y los Functional Requirements de `spec.md`. Los nombres de
campo son conceptuales (capa `domain/model`, ver `plan.md` § Project Structure); el mapeo JPA
concreto (`infrastructure/persistence/model`, entidades `@Entity`) es un detalle de implementación
y no está fijado aquí — domain model y modelo de persistencia son clases distintas por diseño
(Principio I de la constitution), unidas por un mapper en el adapter de persistencia.

## Paciente (referencia externa — no gestionada por esta historia)

Representa la cuenta ya existente del paciente autenticado. Esta historia **no** crea, modifica ni
persiste esta entidad; solo la consume como referencia (ver Assumptions del spec).

| Campo | Tipo | Notas |
|---|---|---|
| `id` | identificador único | proviene del mecanismo de autenticación existente |
| `nombreCompleto` | texto | usado para mostrar/confirmar la cita |
| `documentoIdentificacion` | texto | usado para identificar unívocamente al paciente |
| `numeroWhatsApp` | texto | usado como destino de la notificación (FR-006, FR-014) |

## Médico (entidad de solo lectura para esta historia)

A diferencia de `Paciente`, esta historia **sí** necesita leer `Médico` (endpoint `GET /medicos` del
contrato, FR-001): se modela como una entidad de solo lectura, poblada vía el seed
`db/data-sample.sql` (ver `plan.md` § Project Structure). Esta historia no crea, edita ni administra
médicos ni su agenda (Assumptions) — solo los consulta a través de `MedicoRepositoryPort`.

| Campo | Tipo | Notas |
|---|---|---|
| `id` | identificador único | |
| `nombre` | texto | mostrado al paciente al elegir médico |
| `agenda` | colección de `FranjaHoraria` | la creación/administración de la agenda está fuera de alcance (Assumptions); solo se consulta |

**Validation rules**:
- Consultar franjas de un `medicoId` que no existe MUST resultar en un error "médico no encontrado"
  (contrato `citasalud-api.yaml`, respuesta 404 `MedicoNoEncontrado`).

## FranjaHoraria

Unidad reservable de la agenda de un médico.

| Campo | Tipo | Notas |
|---|---|---|
| `id` | identificador único | |
| `medicoId` | referencia a Médico | |
| `fechaHora` | fecha+hora | MUST ser futura para ser ofrecida como disponible (FR-002) |
| `estado` | enum: `DISPONIBLE`, `BLOQUEADA_TEMPORALMENTE`, `OCUPADA` | ver Lifecycle abajo |
| `bloqueadaPorPacienteId` | referencia a Paciente, nullable | solo con valor cuando `estado = BLOQUEADA_TEMPORALMENTE` |
| `bloqueadaHasta` | fecha+hora, nullable | instante en que expira el bloqueo temporal (FR-010); `fechaHoraSeleccion + 5 minutos` |

**Validation rules**:
- Solo puede pasar a `BLOQUEADA_TEMPORALMENTE` si su estado actual es `DISPONIBLE` (FR-009);
  actualización atómica condicionada por estado (ver `research.md` §5).
- Solo puede pasar a `OCUPADA` si `estado = BLOQUEADA_TEMPORALMENTE`, `bloqueadaPorPacienteId`
  coincide con el paciente que confirma, y `bloqueadaHasta` aún no venció (FR-008, FR-013).
- Restricción de unicidad en `(medicoId, fechaHora)` a nivel de fila `OCUPADA` — nunca puede haber
  dos citas confirmadas sobre la misma franja (SC-003).
- Una franja con `estado = BLOQUEADA_TEMPORALMENTE` y `bloqueadaHasta` vencida se trata como
  `DISPONIBLE` en toda lectura/escritura (liberación perezosa, ver `research.md` §6), aunque el
  valor persistido de `estado` no se haya actualizado todavía.

**Lifecycle (estados)**:

```text
DISPONIBLE ──(seleccionar, FR-009)──> BLOQUEADA_TEMPORALMENTE
BLOQUEADA_TEMPORALMENTE ──(confirmar antes de expirar, FR-011)──> OCUPADA
BLOQUEADA_TEMPORALMENTE ──(expira sin confirmar, FR-010)──> DISPONIBLE
```

No existe transición de `OCUPADA` a otro estado en esta historia (cancelación/reprogramación están
fuera de alcance, ver Assumptions del spec).

## Cita

Registro que resulta de una confirmación exitosa (FR-011).

| Campo | Tipo | Notas |
|---|---|---|
| `id` | identificador único | |
| `pacienteId` | referencia a Paciente | dueño de la cita (FR-011, usado para el límite FR-018) |
| `medicoId` | referencia a Médico | |
| `franjaHorariaId` | referencia a FranjaHoraria | 1:1 — cada `Cita` ocupa exactamente una franja |
| `fechaHoraCita` | fecha+hora | copia de `FranjaHoraria.fechaHora` al momento de confirmar, para no depender de cambios posteriores en la agenda |
| `estadoNotificacion` | enum: `PENDIENTE`, `ENVIADA`, `FALLIDA` | resultado del envío de WhatsApp (FR-014/FR-016/FR-017) |
| `intentosNotificacion` | número | cuántos intentos de envío se hicieron (FR-016) |

**Validation rules**:
- Una `Cita` es única por `franjaHorariaId` (heredado de la restricción de unicidad de
  `FranjaHoraria` en estado `OCUPADA`).
- Un `pacienteId` MUST NOT tener más de **2** `Cita` con `fechaHoraCita` en el futuro
  simultáneamente (FR-018, SC-008). Esta regla se valida en el Use Case de confirmación, no como
  restricción de base de datos (requiere contar filas, no una unicidad simple).
- `estadoNotificacion` inicia en `PENDIENTE` al crear la `Cita` y se actualiza de forma asíncrona
  respecto al registro de la cita (FR-017: el registro de la cita nunca depende del resultado de la
  notificación).

**Relationships**:

```text
Paciente (externo) 1 ──── * Cita
Médico   (externo) 1 ──── * FranjaHoraria
FranjaHoraria       1 ──── 0..1 Cita
```
