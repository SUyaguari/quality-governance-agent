# Implementation Plan: Reserva de cita en línea 24/7

**Branch**: `001-reserva-cita-online-24-7` | **Date**: 2026-07-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-reserva-cita-online-24-7/spec.md`

## Summary

Permitir que un paciente ya autenticado consulte médicos y franjas horarias disponibles, bloquee
temporalmente una franja (5 minutos) y confirme una cita, todo de forma autoservicio 24/7 sin
llamada telefónica. Al confirmarse, el sistema registra la cita y envía una confirmación por
WhatsApp al número del perfil del paciente, con reintentos automáticos ante fallos de envío que
nunca revierten el registro. Un paciente no puede tener más de 2 citas activas simultáneas.

Enfoque técnico: servicio backend Spring Boot (Java 25) existente, implementado en Clean
Architecture (`domain` / `application` / `infrastructure`) por mandato de la constitution del
proyecto. La API se diseña API First: el contrato en `contracts/citasalud-api.yaml` es la fuente de
verdad, generado hacia código vía `openapi-generator`; el dominio nunca depende de ese código
generado. La concurrencia sobre una misma franja se resuelve con escrituras condicionales a nivel
de base de datos relacional (sin locking distribuido, por YAGNI — ver `research.md` §5). Todas las
pruebas (unitarias, integración, funcionales) siguen convención BDD (Given-When-Then) con JUnit 5,
y JaCoCo aplica los umbrales de cobertura obligatorios del proyecto.

## Technical Context

**Language/Version**: Java 25 (toolchain ya configurado en `build.gradle`)

**Primary Dependencies**: Spring Boot 4.1.0 (`spring-boot-starter-webmvc`,
`spring-boot-starter-data-jpa`, `spring-boot-h2console`), Lombok (solo en
`infrastructure`/`persistence` por restricción de la constitution), `openapi-generator-gradle-plugin`
(nuevo — Principio IV), plugin `jacoco` (nuevo — Principio V), `spring-boot-starter-security`
(nuevo — FR-005), `spring-retry` + `spring-boot-starter-aop` (nuevo — FR-016). Ver
`research.md` para el detalle y justificación de cada dependencia nueva.

**Storage**: H2 (relacional, vía Spring Data JPA), única base de datos ya configurada en el
proyecto (`runtimeOnly 'com.h2database:h2'`). Los repositorios se implementan detrás de Ports de
dominio, por lo que sustituir H2 por otro motor relacional en el futuro no afecta `domain` ni
`application`.

**Testing**: JUnit 5 + Mockito + AssertJ (ya disponibles vía `spring-boot-starter-*-test`),
con convención BDD Given-When-Then explícita (Principio II; ver `research.md` §1 sobre por qué no
se introduce Cucumber-JVM en esta historia). `@DataJpaTest` para integración de persistencia,
`MockMvc`/`@SpringBootTest(webEnvironment = RANDOM_PORT)` para integración web y pruebas
funcionales end-to-end de los 6 Acceptance Scenarios del spec.

**Target Platform**: Servicio backend JVM (Spring Boot Web MVC), sin frontend en este repositorio.

**Project Type**: Proyecto único (backend web-service), paquete raíz `org.ups.citasalud`.

**Performance Goals**: No especificados por el spec (Success Criteria son funcionales, no de
latencia/throughput); se asumen expectativas estándar de una API REST síncrona sobre una base de
datos relacional local, sin metas numéricas impuestas por esta historia.

**Constraints**: Clean Architecture con Dependency Rule estricta (Principio I); BDD obligatorio en
3 niveles (Principio II); SOLID/YAGNI/DRY (Principio III); contrato OpenAPI + openapi-generator
obligatorios, sin ediciones manuales al código generado (Principio IV); cobertura JaCoCo >80% por
clase y ≥80% global, build MUST fallar si no se cumple (Principio V); secuencia de desarrollo
obligatoria (contrato → generación → capas → BDD en rojo → implementación SOLID/YAGNI/DRY en verde
→ verificación) y Puertas Obligatorias (compilación, tests+JaCoCo en local y CI, verificación de
los 5 principios por PR, justificación de violaciones en Complexity Tracking, código generado sin
ediciones manuales).

**Scale/Scope**: Una única historia de usuario (US-01, 8 pts), alcance acotado a
consultar/bloquear/confirmar una cita y su notificación; explícitamente excluye gestión de agendas
médicas, cancelación/reprogramación, y el registro/login de cuentas de paciente (asumidos como
prerrequisito externo).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | Principio / Gate | Evaluación | Estado |
|---|---|---|---|
| I | Clean Architecture — Dependency Rule | El diseño (ver Project Structure) separa `domain`, `application` e `infrastructure`; `domain`/`application` no importan Spring, JPA ni código generado por openapi-generator. Los repositorios y el envío de WhatsApp se acceden vía Ports implementados en `infrastructure`. | PASS |
| II | Testing BDD (unitario + integración + funcional) | `research.md` §1 fija JUnit 5 con convención Given-When-Then para los 3 niveles; los 6 Acceptance Scenarios del spec se cubren como pruebas funcionales (`quickstart.md`). | PASS |
| III | SOLID / YAGNI / DRY | Decisiones de `research.md` evitan infraestructura no solicitada (sin Cucumber, sin locking distribuido, sin cola de mensajería) — aplicación explícita de YAGNI. Se verificará en revisión de código durante la implementación. | PASS (verificación continua en code review) |
| IV | API First + OpenAPI + openapi-generator (NON-NEGOTIABLE) | `contracts/citasalud-api.yaml` se crea en Phase 1 antes de cualquier código; `research.md` §2 fija el plugin y el modo de generación (interfaces + DTOs, aislados en `infrastructure/web/generated`). | PASS |
| V | Cobertura JaCoCo (>80% clase, ≥80% global) | `research.md` §3 fija el plugin `jacoco` y las reglas de verificación, con exclusión documentada del paquete generado; `check` depende de `jacocoTestCoverageVerification`. | PASS (a verificar en `tasks.md`/implementación) |
| — | Flujo de Desarrollo (secuencia obligatoria) | Este plan y `tasks.md` (Phase 2) MUST ordenar las tareas como: contrato → generación → esqueleto de capas → pruebas BDD en rojo → implementación en verde → verificación JaCoCo. No se generará código de implementación en esta fase de planificación. | PASS (a aplicar en `tasks.md`) |
| — | Puertas Obligatorias | Compilación y tests+JaCoCo se verifican vía `./gradlew build check`; en local por el desarrollador, y en CI vía el workflow `.github/workflows/ci.yml` (tarea T007 de `tasks.md`); no hay violaciones a justificar en Complexity Tracking. | PASS |

**Riesgo documentado (no es una violación de la constitution)**: FR-005 exige sesión autenticada,
pero el proyecto no tiene hoy un mecanismo de login implementado, y su construcción completa está
fuera de alcance de US-01 (ver Assumptions del spec y `research.md` §4). `tasks.md` MUST incluir una
tarea mínima de autenticación (o un fixture de sesión de prueba) suficiente para que esta historia
sea verificable end-to-end, sin construir el flujo de registro de cuentas.

## Project Structure

### Documentation (this feature)

```text
specs/001-reserva-cita-online-24-7/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md         # Phase 1 output (/speckit-plan command)
├── quickstart.md         # Phase 1 output (/speckit-plan command)
├── contracts/
│   └── citasalud-api.yaml   # Phase 1 output (/speckit-plan command)
├── checklists/
│   └── requirements.md
└── tasks.md              # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
src/main/java/org/ups/citasalud/
├── domain/
│   ├── model/                  # MODELOS DE DOMINIO: FranjaHoraria, Cita, enums de estado
│   │                           # (POJOs puros, sin anotaciones de Spring/JPA — Principio I)
│   ├── port/                   # FranjaHorariaRepositoryPort, CitaRepositoryPort,
│   │                           # MedicoRepositoryPort, PacienteAutenticadoPort (expone id +
│   │                           # nombreCompleto/documentoIdentificacion/numeroWhatsApp del
│   │                           # perfil), NotificacionCitaPort, RelojPort
│   └── exception/               # FranjaNoDisponibleException, LimiteCitasActivasExcedidoException,
│                                 # MedicoNoEncontradoException, ...
├── application/
│   └── usecase/                 # SERVICIOS DE APLICACIÓN (Use Cases): ConsultarFranjasDisponiblesUseCase,
│                                 # ListarMedicosUseCase, BloquearFranjaUseCase, ConfirmarCitaUseCase,
│                                 # ListarMisCitasActivasUseCase — POJOs sin anotaciones de Spring
│                                 # (Principio I); se registran como beans vía @Bean en
│                                 # infrastructure/config/UseCaseConfig, no con @Service.
└── infrastructure/
    ├── web/
    │   ├── generated/            # Código generado por openapi-generator — NUNCA editar a mano
    │   └── (controllers reales que implementan las interfaces generadas + mappers DTO↔dominio)
    ├── persistence/
    │   ├── model/                 # MODELOS DE PERSISTENCIA: entidades @Entity JPA
    │   │                          # (FranjaHorariaJpaEntity, CitaJpaEntity, MedicoJpaEntity) —
    │   │                          # distintas de los modelos de domain/model; el mapeo entre ambos
    │   │                          # vive aquí, no en domain/application (Principio I).
    │   └── (Spring Data JPA repositories + adapters que implementan los *RepositoryPort,
    │        incluyendo MedicoRepositoryPort de solo lectura)
    ├── notification/               # Adapter de NotificacionCitaPort (WhatsApp, con @Retryable)
    ├── security/                   # Configuración de Spring Security / PacienteAutenticadoPort
    └── config/                     # Wiring de beans, configuración de openapi-generator/JaCoCo

src/main/resources/
├── openapi/
│   └── citasalud-api.yaml         # Copia del contrato consumida por el plugin de Gradle en build time
└── db/
    └── data-sample.sql            # DATOS DE EJEMPLO (seed): médicos y franjas horarias futuras
                                    # de prueba, cargados vía Spring SQL init sobre H2 para poder
                                    # ejecutar quickstart.md y las pruebas manuales/funcionales.
                                    # No es un framework de migraciones (Flyway/Liquibase) — se
                                    # evalúa introducirlo solo si una historia futura lo requiere
                                    # (YAGNI, Principio III).

src/test/java/org/ups/citasalud/
├── domain/                        # PRUEBAS UNITARIAS (BDD) de modelos/reglas de dominio,
│                                   # sin Spring context, sin base de datos.
├── application/                   # PRUEBAS UNITARIAS (BDD) de los Use Cases/servicios,
│                                   # con los Ports doblados (mocks/stubs) — sin Spring context.
├── infrastructure/
│   ├── persistence/                # PRUEBAS DE INTEGRACIÓN (@DataJpaTest) de los modelos JPA
│   │                                # y repositorios contra H2 real.
│   ├── web/                        # PRUEBAS DE INTEGRACIÓN de controllers (MockMvc) contra los
│   │                                # DTOs generados por openapi-generator.
│   └── notification/                # PRUEBAS DE INTEGRACIÓN del adapter WhatsApp (stub HTTP del
│                                     # proveedor, valida reintentos de FR-016).
└── functional/                     # PRUEBAS FUNCIONALES end-to-end (@SpringBootTest, MockMvc o
                                     # cliente HTTP real) de los 6 Acceptance Scenarios del spec,
                                     # contra la API completa + H2 + datos de db/data-sample.sql.
```

**Structure Decision**: Proyecto único (backend Spring Boot ya existente), organizado en paquetes
Java que reflejan las capas de Clean Architecture (`domain`, `application`, `infrastructure`) dentro
del paquete raíz `org.ups.citasalud`, en vez del layout genérico `src/models|services|...` de la
plantilla — ese layout no aplica a un proyecto Gradle/Java de un solo módulo. Confirmado
explícitamente:

- **Modelos**: existen en dos niveles, por diseño de Clean Architecture — `domain/model` (modelos
  de negocio, sin anotaciones) y `infrastructure/persistence/model` (entidades `@Entity` JPA). No
  son la misma clase para no acoplar el dominio a JPA (Principio I); el mapeo entre ambos se hace en
  el adapter de `infrastructure/persistence`.
- **Servicios**: sí existen — son los Use Cases de `application/usecase`. En Clean Architecture se
  llaman "Use Cases"; funcionalmente cumplen el mismo rol que una capa de "servicios" en una
  arquitectura en capas tradicional. Son POJOs sin `@Service` ni ninguna otra anotación de Spring
  (Principio I); se registran como beans vía `@Bean` en `infrastructure/config/UseCaseConfig`.
- **Carpeta de datos de ejemplo**: `src/main/resources/db/data-sample.sql`, con médicos y franjas
  horarias de prueba para poder ejecutar `quickstart.md` sin depender de una base de datos externa.
- **Pruebas**: sí habrá pruebas unitarias (`domain/`, `application/`), de integración
  (`infrastructure/persistence`, `infrastructure/web`, `infrastructure/notification`) y funcionales
  (`functional/`), en línea con los tres niveles que exige el Principio II (BDD Testing) de la
  constitution.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| *(ninguna)* | El Constitution Check no registró incumplimientos — tabla vacía intencionalmente, en el formato del template para consistencia futura. | — |
