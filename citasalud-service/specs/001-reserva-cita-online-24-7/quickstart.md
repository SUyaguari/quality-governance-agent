# Quickstart: Validar "Reserva de cita en línea 24/7"

**Feature**: `001-reserva-cita-online-24-7`

Esta guía valida el flujo end-to-end descrito en `spec.md` (Flujo principal + Escenarios 1-6),
usando el contrato en `contracts/citasalud-api.yaml` y el modelo en `data-model.md`. No incluye
código de implementación — eso corresponde a `tasks.md` y a la fase de implementación.

## Prerrequisitos

- JDK 25 (toolchain configurado en `build.gradle`).
- Una cuenta de paciente de prueba ya existente y autenticable (ver `research.md` §4 — el
  registro/login en sí no es parte de esta historia; se asume un usuario semilla, p. ej. cargado
  vía un `data.sql`/`schema.sql` de prueba, o un endpoint de test-only, decidido en `tasks.md`).
- Al menos un médico con franjas horarias futuras cargadas en la base de datos de prueba — provisto
  por el seed en `src/main/resources/db/data-sample.sql` (ver `plan.md`, Project Structure).
- `./gradlew build` MUST completar sin errores (Puerta Obligatoria: Compilación) y
  `./gradlew check` MUST pasar, incluyendo `jacocoTestCoverageVerification` (Puerta Obligatoria:
  Cobertura JaCoCo).

## Levantar el servicio

```bash
./gradlew bootRun
```

El servicio expone la API bajo `/api/v1` según `contracts/citasalud-api.yaml`, y la consola H2 (si
está habilitada) en `/h2-console` para inspeccionar el estado de `franja_horaria` y `cita` durante
la validación manual.

## Escenario 1 — Reserva exitosa fuera de horario de atención telefónica (Acceptance Scenario 1)

1. Iniciar sesión con la cuenta de paciente de prueba (fuera de alcance de esta historia definir
   el mecanismo exacto; ver `research.md` §4).
2. `GET /medicos` → obtener un `medicoId`.
3. `GET /medicos/{medicoId}/franjas` → confirmar que solo aparecen franjas futuras con
   `estado = DISPONIBLE` (FR-001, FR-002).
4. `POST /franjas/{franjaId}/bloqueos` → esperar `201` con `bloqueadaHasta` ≈ ahora + 5 minutos
   (FR-009, FR-010).
5. `POST /citas` con `{"franjaId": "..."}` → esperar `201` con la `Cita` creada y
   `estadoNotificacion` en `PENDIENTE` o `ENVIADA` (FR-011, FR-014).
6. Verificar en logs/estado interno que se intentó (o se intentará vía reintento) el envío de
   WhatsApp al número del perfil del paciente (FR-006, FR-016), y que la respuesta del paso 5 no
   quedó bloqueada esperando ese envío (FR-017, SC-007).

**Resultado esperado**: cita registrada; intento de notificación WhatsApp disparado. Corresponde a
Acceptance Scenario 1 y SC-001, SC-005.

## Escenario 2 — Franja ya ocupada (Acceptance Scenario 2)

1. Repetir pasos 1-5 del Escenario 1 hasta confirmar una cita sobre una franja `F1`.
2. Con una segunda cuenta de paciente de prueba, `GET /medicos/{medicoId}/franjas` → `F1` MUST NOT
   aparecer en la lista (ya está `OCUPADA`, FR-004).
3. Intentar `POST /franjas/{F1}/bloqueos` directamente → esperar `409` con
   `code = FRANJA_NO_DISPONIBLE`.

**Resultado esperado**: la franja se muestra como no disponible y el intento de bloqueo/confirmación
se rechaza. Corresponde a Acceptance Scenario 2 y SC-003, SC-004.

## Escenario 3 — Bloqueo temporal concurrente (Acceptance Scenario 3)

1. Con el paciente A, `POST /franjas/{F2}/bloqueos` sobre una franja libre `F2` → `201`.
2. Inmediatamente, con el paciente B, intentar `POST /franjas/{F2}/bloqueos` sobre la misma `F2` →
   esperar `409` con `code = FRANJA_NO_DISPONIBLE`, mientras el bloqueo de A siga vigente
   (< 5 minutos).

**Resultado esperado**: solo un paciente puede tener el bloqueo activo sobre una franja a la vez.
Corresponde a Acceptance Scenario 3 y SC-003.

## Escenario 4 — Acceso sin sesión iniciada (Acceptance Scenario 4)

1. Sin autenticar, `GET /medicos/{medicoId}/franjas` → esperar `401` con `code = NO_AUTENTICADO`.
2. Sin autenticar, `POST /citas` → esperar `401`.

**Resultado esperado**: ningún endpoint de consulta/selección/confirmación es accesible sin sesión.
Corresponde a Acceptance Scenario 4 y SC-006.

## Escenario 5 — Expiración del bloqueo temporal (Acceptance Scenario 5)

1. `POST /franjas/{F3}/bloqueos` sobre una franja libre `F3` → `201` con `bloqueadaHasta`.
2. Esperar más de 5 minutos (o, en pruebas automatizadas, usar un reloj simulado/`Clock` inyectado
   — ver `research.md` §6) sin confirmar.
3. `POST /citas` con `{"franjaId": "F3"}` → esperar `409` con `code = FRANJA_NO_DISPONIBLE`.
4. `GET /medicos/{medicoId}/franjas` → `F3` MUST volver a aparecer como disponible.

**Resultado esperado**: la franja se libera automáticamente tras 5 minutos sin confirmar.
Corresponde a Acceptance Scenario 5, FR-010 y SC-004.

## Escenario 6 — Límite de citas activas (Acceptance Scenario 6)

1. Con un paciente que ya tiene 2 citas activas confirmadas (repetir Escenario 1 dos veces),
   bloquear una tercera franja libre `F4` y `POST /citas` con `{"franjaId": "F4"}`.
2. Esperar `409` con `code = LIMITE_CITAS_ACTIVAS_EXCEDIDO` (FR-018).

**Resultado esperado**: la tercera confirmación se rechaza sin afectar las 2 citas ya registradas.
Corresponde a Acceptance Scenario 6 y SC-008.

## Verificación de Puertas Obligatorias (constitution)

- `./gradlew build` compila sin errores.
- `./gradlew check` pasa, incluyendo pruebas unitarias/integración/funcionales (BDD,
  Given-When-Then) y `jacocoTestCoverageVerification` (>80% por clase, ≥80% global, excluyendo el
  paquete generado por openapi-generator).
- El contrato `contracts/citasalud-api.yaml` (o su copia en
  `src/main/resources/openapi/citasalud-api.yaml`) es la única fuente de los DTOs/interfaces bajo
  `infrastructure/web/generated`; ningún archivo de ese paquete debe tener ediciones manuales.
