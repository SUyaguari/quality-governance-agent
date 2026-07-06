---

description: "Task list for Reserva de cita en línea 24/7 (US-01)"
---

# Tasks: Reserva de cita en línea 24/7

**Input**: Design documents from `/specs/001-reserva-cita-online-24-7/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/citasalud-api.yaml, quickstart.md

**Tests**: MANDATORY — la constitution del proyecto (Principio II, BDD Testing) exige pruebas
unitarias, de integración y funcionales en estilo Given-When-Then para todo comportamiento
observable; no son opcionales en este proyecto. El orden de las fases respeta la secuencia
obligatoria de la constitution: contrato OpenAPI → generación con openapi-generator → esqueleto en
capas → pruebas BDD en rojo → implementación SOLID/YAGNI/DRY en verde → verificación JaCoCo.

**Organization**: Solo existe una historia de usuario en `spec.md` (US1, P1), por lo que todas las
tareas de negocio caen en la Fase 3. Las fases de Setup y Foundational cubren el tooling obligatorio
por la constitution (openapi-generator, JaCoCo, CI, seguridad, datos de ejemplo) que no pertenece a
ninguna historia en particular.

> **Nota de revisión (`/speckit-analyze`, 2026-07-02)**: esta versión incorpora la remediación de
> los hallazgos de dos pasadas de análisis:
> - **C1** (faltaba pipeline de CI → T007), **G1** (`Médico` no tenía Port/Entity/Use Case pese a
>   ser requerido por FR-001 y el contrato → T014, T029-T030, T032, T036-T037, T039, T020) y
>   **U1** (`PacienteAutenticadoPort` no exponía datos de perfil, necesarios para FR-006/FR-014 →
>   T008, T034, T038).
> - **G2** (`ListarMisCitasActivasUseCase` sin test unitario dedicado → nuevo T017), **G3** (FR-012,
>   doble confirmación accidental, ya cubierto en T016 desde la ronda anterior), **G4** (SC-007,
>   cita válida tras fallo definitivo de WhatsApp, ya cubierto en T019/T020 desde la ronda
>   anterior), **A1** (placeholder `[NEEDS CLARIFICATION]` de reintentos reescrito como regla en
>   `spec.md`), **I1** (nota aclaratoria sobre el salto FR-006→FR-008 en `spec.md`), **I2** (tabla
>   de Complexity Tracking restaurada en `plan.md`) y **U2** (citas explícitas de FR-003/FR-008/
>   FR-014/FR-015 agregadas a las tareas correspondientes).
>
> Los IDs de tareas fueron renumerados respecto a versiones anteriores de este archivo.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede ejecutarse en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: US1 (única historia de este feature)
- Rutas de archivo exactas en cada descripción, bajo `org.ups.citasalud` (ver `plan.md` § Project Structure)

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar el build, el CI y el esqueleto de paquetes antes de cualquier código de negocio.

- [x] T001 Actualizar `build.gradle`: agregar plugins `org.openapi.generator` y `jacoco`, y
      dependencias `spring-boot-starter-security`, `org.springframework.retry:spring-retry`,
      `spring-boot-starter-aop` (ver `research.md` § "Resumen de dependencias nuevas")
- [x] T002 [P] Copiar `specs/001-reserva-cita-online-24-7/contracts/citasalud-api.yaml` a
      `src/main/resources/openapi/citasalud-api.yaml` (fuente única para el build, `research.md` §2)
- [x] T003 [P] Configurar la tarea `openApiGenerate` en `build.gradle` (generator `spring`,
      `interfaceOnly=true`, `useSpringBoot3=true`, `inputSpec` apuntando a
      `src/main/resources/openapi/citasalud-api.yaml`, `apiPackage` y `modelPackage` bajo
      `org.ups.citasalud.infrastructure.web.generated`) y hacer que `compileJava` dependa de ella
- [x] T004 [P] Configurar el plugin `jacoco` en `build.gradle`: `jacocoTestCoverageVerification`
      con reglas `CLASS` (mínimo 0.80) y `BUNDLE` (mínimo 0.80), excluyendo
      `org.ups.citasalud.infrastructure.web.generated.**`, y hacer que `check` dependa de esa
      verificación (`research.md` §3, Principio V de la constitution)
- [x] T005 [P] Crear el esqueleto de paquetes vacío bajo `src/main/java/org/ups/citasalud/`:
      `domain/model`, `domain/port`, `domain/exception`, `application/usecase`,
      `infrastructure/web`, `infrastructure/persistence/model`, `infrastructure/notification`,
      `infrastructure/security`, `infrastructure/config`
- [x] T006 [P] Crear `src/main/resources/db/data-sample.sql` con datos de ejemplo (médicos y
      franjas horarias futuras) para `quickstart.md` y las pruebas funcionales
- [x] T007 [P] Crear el workflow de CI en `.github/workflows/ci.yml` que ejecute
      `./gradlew build check` (incluye `jacocoTestCoverageVerification`) en cada push/PR sobre
      `main` y sobre la rama del feature, con JDK 25, satisfaciendo la Puerta Obligatoria de
      verificación en CI (constitution, "Puertas Obligatorias") — **ya creado** en este repo,
      revisar/ajustar si cambian versiones o triggers

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestructura bloqueante que toda la historia US1 necesita.

**⚠️ CRITICAL**: Ninguna tarea de la Fase 3 puede empezar hasta completar esta fase.

- [x] T008 Configurar autenticación mínima basada en sesión con Spring Security en
      `src/main/java/org/ups/citasalud/infrastructure/security/`: definir el Port
      `PacienteAutenticadoPort` en `domain/port` — expone el `id` **y los datos de perfil
      necesarios (`nombreCompleto`, `documentoIdentificacion`, `numeroWhatsApp`)** del paciente
      autenticado actual, requeridos por FR-006/FR-011/FR-014 — y su implementación/adapter en
      `infrastructure/security`, junto con un proveedor de autenticación mínimo suficiente para
      pruebas (FR-005; riesgo documentado en `plan.md` — no incluye registro/alta de cuentas)
- [x] T009 [P] Definir `RelojPort` (abstracción de reloj) en
      `src/main/java/org/ups/citasalud/domain/port/RelojPort.java` y su implementación real en
      `src/main/java/org/ups/citasalud/infrastructure/config/RelojSistema.java`, para poder simular
      el paso del tiempo en las pruebas de expiración del bloqueo (FR-010, Escenario 5)
- [x] T010 Configurar carga de `db/data-sample.sql` vía `spring.sql.init.mode` en
      `src/main/resources/application.yml` para los perfiles de desarrollo/test

**Checkpoint**: Foundation lista — puede empezar la implementación de US1.

---

## Phase 3: User Story 1 - Reservar una cita médica en línea sin depender del horario telefónico (Priority: P1) 🎯 MVP

**Goal**: Un paciente autenticado consulta médicos/franjas, bloquea temporalmente una franja,
confirma la cita y recibe una confirmación por WhatsApp — todo a través de la API, sin llamar ni
depender de personal del centro médico (spec.md, User Story 1, FR-015).

**Independent Test**: Ejecutar el flujo completo de `quickstart.md` (Escenarios 1-6) contra el
servicio levantado con `./gradlew bootRun`, usando los datos de `db/data-sample.sql`.

### Tests for User Story 1 (BDD — MANDATORY, escribir primero y confirmar que fallan en rojo)

- [x] T011 [P] [US1] Prueba unitaria BDD de transiciones de estado de `FranjaHoraria`
      (DISPONIBLE→BLOQUEADA_TEMPORALMENTE→OCUPADA, expiración tras 5 min, FR-009/FR-010) en
      `src/test/java/org/ups/citasalud/domain/model/FranjaHorariaTest.java`
- [x] T012 [P] [US1] Prueba unitaria BDD de invariantes de `Cita` (única por franja, FR-011) en
      `src/test/java/org/ups/citasalud/domain/model/CitaTest.java`
- [x] T013 [P] [US1] Prueba unitaria BDD de `ConsultarFranjasDisponiblesUseCase` (solo franjas
      futuras y disponibles, FR-001/FR-002; médico inexistente → `MedicoNoEncontradoException`) con
      Ports doblados en
      `src/test/java/org/ups/citasalud/application/usecase/ConsultarFranjasDisponiblesUseCaseTest.java`
- [x] T014 [P] [US1] Prueba unitaria BDD de `ListarMedicosUseCase` (lista médicos existentes, FR-001)
      en `src/test/java/org/ups/citasalud/application/usecase/ListarMedicosUseCaseTest.java`
- [x] T015 [P] [US1] Prueba unitaria BDD de `BloquearFranjaUseCase` (selección y bloqueo exitoso de
      una franja disponible, FR-003/FR-009; franja no disponible → `FranjaNoDisponibleException`,
      FR-004) en
      `src/test/java/org/ups/citasalud/application/usecase/BloquearFranjaUseCaseTest.java`
- [x] T016 [P] [US1] Prueba unitaria BDD de `ConfirmarCitaUseCase` (confirmación permitida solo si
      la franja sigue bloqueada a favor del paciente, FR-008; usa el número de WhatsApp del perfil
      vía `PacienteAutenticadoPort`, FR-006; bloqueo expirado → rechazo FR-013; solicitud duplicada
      de la misma confirmación → nunca dos citas, FR-012; límite de 2 citas activas → rechazo
      FR-018; dispara notificación sin bloquear el registro, FR-017) en
      `src/test/java/org/ups/citasalud/application/usecase/ConfirmarCitaUseCaseTest.java`
- [x] T017 [P] [US1] Prueba unitaria BDD de `ListarMisCitasActivasUseCase` (lista solo citas del
      paciente autenticado con franja horaria futura; excluye citas de otros pacientes, FR-018) en
      `src/test/java/org/ups/citasalud/application/usecase/ListarMisCitasActivasUseCaseTest.java`
- [x] T018 [P] [US1] Prueba de integración BDD (`@DataJpaTest`) del adapter de
      `FranjaHorariaRepositoryPort`: escritura condicional atómica de bloqueo/confirmación y
      restricción de unicidad `(medicoId, fechaHora)` en estado OCUPADA (FR-009/FR-011,
      `research.md` §5) en
      `src/test/java/org/ups/citasalud/infrastructure/persistence/FranjaHorariaRepositoryAdapterTest.java`
- [x] T019 [P] [US1] Prueba de integración BDD (`@DataJpaTest`) del adapter de
      `CitaRepositoryPort`: conteo de citas activas por paciente (FR-018) en
      `src/test/java/org/ups/citasalud/infrastructure/persistence/CitaRepositoryAdapterTest.java`
- [x] T020 [P] [US1] Prueba de integración BDD del adapter `NotificacionCitaPort` (WhatsApp): envía
      la confirmación al número del perfil (FR-014); reintentos automáticos ante fallo simulado del
      proveedor (FR-016); la `Cita` permanece válida/consultable tras agotar los reintentos con
      fallo definitivo (SC-007); con un stub HTTP del proveedor en
      `src/test/java/org/ups/citasalud/infrastructure/notification/NotificacionCitaAdapterTest.java`
- [x] T021 [P] [US1] Prueba de integración BDD (`MockMvc`) de los controllers contra los DTOs
      generados: `GET /medicos` (200), `GET /medicos/{id}/franjas` con médico inexistente (404
      `MedicoNoEncontrado`), 401 sin sesión (FR-005), 404 franja, 409 franja no disponible en
      `src/test/java/org/ups/citasalud/infrastructure/web/CitasControllerTest.java`
- [x] T022 [US1] Prueba funcional BDD (`@SpringBootTest`) del Acceptance Scenario 1 (reserva
      exitosa fuera de horario telefónico) en
      `src/test/java/org/ups/citasalud/functional/ReservaCitaExitosaTest.java`
- [x] T023 [US1] Prueba funcional BDD del Acceptance Scenario 2 (franja ya ocupada) en
      `src/test/java/org/ups/citasalud/functional/FranjaYaOcupadaTest.java`
- [x] T024 [US1] Prueba funcional BDD del Acceptance Scenario 3 (bloqueo temporal concurrente
      entre dos pacientes) en
      `src/test/java/org/ups/citasalud/functional/BloqueoConcurrenteTest.java`
- [x] T025 [US1] Prueba funcional BDD del Acceptance Scenario 4 (acceso sin sesión iniciada) en
      `src/test/java/org/ups/citasalud/functional/AccesoSinSesionTest.java`
- [x] T026 [US1] Prueba funcional BDD del Acceptance Scenario 5 (expiración del bloqueo temporal,
      usando `RelojPort` simulado) en
      `src/test/java/org/ups/citasalud/functional/ExpiracionBloqueoTest.java`
- [x] T027 [US1] Prueba funcional BDD del Acceptance Scenario 6 (límite de 2 citas activas) en
      `src/test/java/org/ups/citasalud/functional/LimiteCitasActivasTest.java`

**Checkpoint**: T011-T027 existen y MUST fallar (rojo) antes de continuar con la implementación.

### Implementation for User Story 1

- [x] T028 [P] [US1] Crear el modelo de dominio `FranjaHoraria` (estado, bloqueadaPorPacienteId,
      bloqueadaHasta, reglas de transición) en
      `src/main/java/org/ups/citasalud/domain/model/FranjaHoraria.java`
- [x] T029 [P] [US1] Crear el modelo de dominio `Cita` (estadoNotificacion, intentosNotificacion)
      en `src/main/java/org/ups/citasalud/domain/model/Cita.java`
- [x] T030 [P] [US1] Crear las excepciones de dominio `FranjaNoDisponibleException`,
      `LimiteCitasActivasExcedidoException` y `MedicoNoEncontradoException` en
      `src/main/java/org/ups/citasalud/domain/exception/`
- [x] T031 [P] [US1] Definir los Ports `FranjaHorariaRepositoryPort`, `CitaRepositoryPort`,
      `MedicoRepositoryPort` y `NotificacionCitaPort` en
      `src/main/java/org/ups/citasalud/domain/port/`
- [x] T032 [US1] Implementar `ConsultarFranjasDisponiblesUseCase` (FR-001/FR-002) en
      `src/main/java/org/ups/citasalud/application/usecase/ConsultarFranjasDisponiblesUseCase.java`
      — valida existencia del médico vía `MedicoRepositoryPort` (404 si no existe) antes de listar
      franjas (depende de T028, T031)
- [x] T033 [US1] Implementar `ListarMedicosUseCase` (FR-001) en
      `src/main/java/org/ups/citasalud/application/usecase/ListarMedicosUseCase.java` (depende de
      T031)
- [x] T034 [US1] Implementar `BloquearFranjaUseCase` (selección + bloqueo, FR-003/FR-004/FR-009)
      en `src/main/java/org/ups/citasalud/application/usecase/BloquearFranjaUseCase.java` (depende
      de T028, T030, T031, T009)
- [x] T035 [US1] Implementar `ConfirmarCitaUseCase` (confirmación, FR-008/FR-011/FR-012/FR-013/
      FR-017/FR-018) en
      `src/main/java/org/ups/citasalud/application/usecase/ConfirmarCitaUseCase.java` — obtiene
      `nombreCompleto`/`documentoIdentificacion`/`numeroWhatsApp` del paciente autenticado vía
      `PacienteAutenticadoPort` (FR-006) para registrar la cita y disparar la notificación (depende
      de T008, T029, T030, T031, T009)
- [x] T036 [US1] Implementar `ListarMisCitasActivasUseCase` en
      `src/main/java/org/ups/citasalud/application/usecase/ListarMisCitasActivasUseCase.java`
      (depende de T029, T031)
- [x] T037 [P] [US1] Crear las entidades JPA `FranjaHorariaJpaEntity`, `CitaJpaEntity` y
      `MedicoJpaEntity` (de solo lectura) más sus mappers domain↔JPA en
      `src/main/java/org/ups/citasalud/infrastructure/persistence/model/`
- [x] T038 [US1] Implementar los repositorios Spring Data JPA y los adapters de
      `FranjaHorariaRepositoryPort`/`CitaRepositoryPort`/`MedicoRepositoryPort` (escritura
      condicional atómica para franjas/citas, `research.md` §5; lectura simple para médicos) en
      `src/main/java/org/ups/citasalud/infrastructure/persistence/` (depende de T037, T031)
- [x] T039 [US1] Implementar el adapter de `NotificacionCitaPort` (envío por WhatsApp al número del
      perfil, FR-014, vía `RestClient` + `@Retryable` para los reintentos de FR-016; usando el
      `numeroWhatsApp` que le pasa `ConfirmarCitaUseCase`) en
      `src/main/java/org/ups/citasalud/infrastructure/notification/NotificacionCitaAdapter.java`
      (depende de T031)
- [x] T040 [US1] Implementar los controllers reales que implementan las interfaces generadas por
      openapi-generator (incluyendo la operación `listarMedicos`), con mappers DTO↔dominio — el
      conjunto de endpoints resuelve todo el flujo sin llamada telefónica (FR-015) — en
      `src/main/java/org/ups/citasalud/infrastructure/web/` (depende de T032-T036, T003)
- [x] T041 [US1] Configurar el wiring de beans (Use Cases, adapters, número de reintentos
      configurable por propiedades) en `src/main/java/org/ups/citasalud/infrastructure/config/`

**Checkpoint**: US1 completa; T011-T027 deben pasar en verde.

---

## Phase 4: Polish & Verificación (Puertas Obligatorias)

**Purpose**: Verificar las puertas obligatorias de la constitution antes de abrir el Pull Request.

- [x] T042 Ejecutar `./gradlew build check` en local y confirmar compilación exitosa y
      `jacocoTestCoverageVerification` en verde (>80% por clase, ≥80% global); confirmar que el
      workflow de CI (T007) pasa en verde sobre el PR
- [x] T043 [P] Revisar cumplimiento de los 5 principios de la constitution en el PR (Clean
      Architecture / Dependency Rule, BDD en 3 niveles, SOLID/YAGNI/DRY, código generado sin
      ediciones manuales, cobertura JaCoCo) y documentar cualquier excepción en la tabla
      "Complexity Tracking" de `plan.md`
- [x] T044 Ejecutar `quickstart.md` de punta a punta contra el servicio levantado
      (`./gradlew bootRun`) para validar manualmente los 6 Acceptance Scenarios

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias — puede iniciar de inmediato
- **Foundational (Phase 2)**: depende de Setup — bloquea toda la Fase 3
- **User Story 1 (Phase 3)**: depende de Foundational; dentro de la fase, Tests (T011-T027) MUST
  escribirse y fallar antes de Implementation (T028-T041)
- **Polish (Phase 4)**: depende de que Phase 3 esté completa y en verde

### Dentro de la Fase 3

- Tests (T011-T027) antes que Implementation (T028-T041)
- Modelos de dominio, excepciones y Ports (T028-T031) antes que Use Cases (T032-T036)
- Use Cases (T032-T036) y modelos JPA (T037) antes que los adapters de persistencia (T038)
- Ports (T031) antes que cualquier adapter (T038, T039)
- `PacienteAutenticadoPort` (T008) antes que `ConfirmarCitaUseCase` (T035)
- Use Cases (T032-T036) y generación OpenAPI (T003) antes que los controllers (T040)
- Todo lo anterior antes del wiring final (T041)

### Parallel Opportunities

- Todas las tareas [P] de Setup (T002-T007) en paralelo, tras T001
- T011-T021 (tests unitarios y de integración) en paralelo entre sí; T022-T027 (funcionales) son
  secuenciales entre sí porque comparten el mismo contexto `@SpringBootTest` y los mismos datos de
  `db/data-sample.sql`
- T028-T031 (modelos, excepciones, Ports) en paralelo entre sí
- T037 en paralelo con T032-T036 (paquetes distintos)

---

## Parallel Example: User Story 1 (Tests)

```bash
# Lanzar en paralelo las pruebas unitarias y de integración de US1:
Task: "Prueba unitaria BDD de FranjaHoraria en src/test/java/org/ups/citasalud/domain/model/FranjaHorariaTest.java"
Task: "Prueba unitaria BDD de Cita en src/test/java/org/ups/citasalud/domain/model/CitaTest.java"
Task: "Prueba unitaria BDD de ConsultarFranjasDisponiblesUseCase en src/test/java/org/ups/citasalud/application/usecase/ConsultarFranjasDisponiblesUseCaseTest.java"
Task: "Prueba unitaria BDD de ListarMedicosUseCase en src/test/java/org/ups/citasalud/application/usecase/ListarMedicosUseCaseTest.java"
Task: "Prueba unitaria BDD de ListarMisCitasActivasUseCase en src/test/java/org/ups/citasalud/application/usecase/ListarMisCitasActivasUseCaseTest.java"
Task: "Prueba de integración BDD de FranjaHorariaRepositoryAdapter en src/test/java/org/ups/citasalud/infrastructure/persistence/FranjaHorariaRepositoryAdapterTest.java"
```

---

## Implementation Strategy

### MVP First (y único alcance de este feature)

1. Completar Phase 1: Setup
2. Completar Phase 2: Foundational (bloqueante)
3. Completar Phase 3: User Story 1 — Tests (rojo) → Implementation (verde)
4. **STOP y VALIDAR**: ejecutar `quickstart.md` completo
5. Completar Phase 4: Polish & Verificación (Puertas Obligatorias) antes de abrir el PR

No hay historias adicionales en este feature (spec.md solo define US1); no aplica estrategia de
entrega incremental multi-historia ni trabajo en paralelo por equipo separado por historia.

---

## Notes

- [P] = archivos distintos, sin dependencias pendientes
- [US1] = única historia de usuario de este feature; todas las tareas de negocio la referencian
- Las pruebas (T011-T027) MUST escribirse y fallar antes de implementar (Principio II,
  NON-NEGOTIABLE en este proyecto — no son opcionales)
- Confirmar en cada tarea de `infrastructure/web/generated` que el código no fue editado a mano
  (Principio IV)
- Hacer commit después de cada tarea o grupo lógico de tareas

## Notas de implementación (post-ejecución, 2026-07-02)

- **T001 ajustado**: `spring-boot-starter-aop` no existe como artefacto en Spring Boot 4.1 (la
  modularización de Boot 4 lo eliminó); `spring-aop` llega transitivamente vía
  `spring-boot-starter-security`. Se agregó `spring-retry:2.0.10` con versión explícita (tampoco
  está en el BOM de Boot 4.1) y, adicionalmente, `spring-boot-starter-validation` (para
  `@NotNull`/`@Valid` del código generado) y `spring-boot-starter-restclient` (para el bean
  `RestClient.Builder`, necesario por `NotificacionCitaAdapter`) — ninguno de los dos estaba
  previsto en `research.md`, ambos son transitivos obligatorios en Boot 4.1.
- **T034/T039 (Spring Retry) ajustado**: `WhatsAppHttpSender` es un `@Component` separado de
  `NotificacionCitaAdapter` porque `@Retryable` de Spring Retry no funciona en auto-invocación
  (llamada desde un método al `this` de la misma clase no pasa por el proxy AOP).
- **Principio I reforzado durante la revisión (T043)**: los 5 Use Cases originalmente se anotaron
  `@Service` por conveniencia; se detectó que esto viola la constitution (Use Cases MUST estar
  libres de anotaciones de framework, igual que las Entities). Se corrigió: los Use Cases son
  POJOs sin anotaciones y se registran como beans vía `@Bean` en el nuevo
  `infrastructure/config/UseCaseConfig.java`.
- **T020 (test de `NotificacionCitaPort`) ajustado**: `@RestClientTest` no existe en Spring Boot
  4.1 (modularizado en `spring-boot-resttestclient`, orientado a `TestRestTemplate`/`RestTestClient`,
  no a `MockRestServiceServer` + slice de contexto). La prueba usa `MockRestServiceServer` de
  `spring-test` directamente, envolviendo `WhatsAppHttpSender` en un proxy AOP manual
  (`RetryInterceptorBuilder`) equivalente al que instalaría `@EnableRetry`, sin levantar contexto
  Spring — más rápido y sin depender de anotaciones inexistentes en esta versión.
- **T021/T022-T027 (nombres de paquete)**: `@DataJpaTest`/`TestEntityManager`/`@WebMvcTest` se
  movieron de `org.springframework.boot.test.autoconfigure.*` a paquetes modularizados
  (`org.springframework.boot.data.jpa.test.autoconfigure`, `org.springframework.boot.jpa.test.autoconfigure`,
  `org.springframework.boot.webmvc.test.autoconfigure`) en Spring Boot 4.1.
- **T022-T027 (aislamiento entre clases)**: se agregó `@DirtiesContext(classMode = AFTER_CLASS)` en
  `FunctionalTestBase` — sin esto, las 6 clases de prueba funcional comparten el mismo contexto
  Spring/H2 cacheado y los pacientes de prueba fijos (`paciente1`/`paciente2`) acumulan citas
  activas entre clases, rompiendo las aserciones de FR-018 (límite de 2).
- **T022-T027 (comportamiento de `TestRestTemplate`)**: en Boot 4.1, `TestRestTemplate` NO lanza
  excepción en respuestas 4xx/5xx (devuelve el `ResponseEntity` con el status de error); las
  pruebas verifican `getStatusCode()`/`getBody()` directamente en vez de `assertThatThrownBy`.
  Se agregó `server.servlet.context-path: /api/v1` a `application.yml` para que las rutas servidas
  coincidan con `servers: /api/v1` del contrato sin tocar los `@RequestMapping` generados.
- **Hallazgo de bug real durante T044 (validación manual con curl)**: `SesionNoIniciadaEntryPoint`
  escribía el cuerpo del 401 con el `PrintWriter` del response sin fijar `characterEncoding`, y el
  contenedor usaba ISO-8859-1 por defecto, corrompiendo acentos (`"sesión"` → `"sesi?n"`). Corregido
  escribiendo a `response.getOutputStream()` con `setCharacterEncoding("UTF-8")` explícito.
- **Prueba adicional no listada originalmente**: se agregó
  `infrastructure/persistence/MedicoRepositoryAdapterTest.java` (`@DataJpaTest`) durante T042/T043
  al detectar que `MedicoRepositoryAdapter.listarTodos()` y `MedicoJpaEntity` no tenían cobertura
  real (solo mockeada), lo que hacía fallar `jacocoTestCoverageVerification` (Principio V).
- **Resultado final**: `./gradlew build check` en verde — 50 pruebas (0 fallos), cobertura global
  99% (umbral ≥80%), sin violaciones de `jacocoTestCoverageVerification` por clase (umbral >80%).
  `org.ups.citasalud.CitasaludServiceApplication` se excluyó explícitamente de las reglas de
  cobertura (bootstrap de una línea, práctica estándar), documentado en `build.gradle`.
