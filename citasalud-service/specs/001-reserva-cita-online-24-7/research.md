# Research: Reserva de cita en línea 24/7

**Feature**: `001-reserva-cita-online-24-7` · **Date**: 2026-07-02

Este documento resuelve las incógnitas técnicas (`NEEDS CLARIFICATION`) del Technical Context de
`plan.md` para la implementación de US-01, dentro de las restricciones fijadas por
`.specify/memory/constitution.md` v1.1.0 (Clean Architecture, BDD, SOLID/YAGNI/DRY, API First +
openapi-generator, JaCoCo).

## 1. Framework de pruebas BDD

- **Decision**: JUnit 5 como motor de ejecución, con nomenclatura y estructura Given-When-Then
  explícita (métodos `@Test` organizados en bloques `given/when/then` o `@Nested` con
  `@DisplayName` en lenguaje de comportamiento), **sin** introducir Cucumber-JVM ni archivos
  `.feature` en esta historia.
- **Rationale**: La Constitución (Principio II) exige BDD en tres niveles (unitario, integración,
  funcional) con nomenclatura orientada a comportamiento, pero no exige un motor Gherkin
  específico. El proyecto ya trae `spring-boot-starter-webmvc-test` y
  `spring-boot-starter-data-jpa-test` (que incluyen JUnit 5, Mockito y AssertJ) sin ninguna
  dependencia de Cucumber. Añadir Cucumber-JVM implicaría una dependencia nueva, un paso de build
  adicional y mantenimiento de `.feature` files para una única historia de 8 puntos — esto viola
  YAGNI (Principio III). JUnit 5 con convención BDD explícita satisface el Principio II sin ese
  costo. Si en historias futuras se requiere trazabilidad Gherkin más formal, se puede introducir
  Cucumber-JVM entonces, sin romper este enfoque (los métodos de test seguirían siendo BDD).
- **Alternatives considered**:
  - *Cucumber-JVM + JUnit 5 Platform Engine*: mayor trazabilidad textual 1:1 con los escenarios
    Gherkin del spec, pero mayor complejidad de build y curva de aprendizaje para un solo caso de
    uso; descartado por YAGNI.
  - *Spock (Groovy)*: BDD nativo muy expresivo, pero introduce Groovy como lenguaje adicional al
    stack (Java 25 puro); descartado por inconsistencia tecnológica.

## 2. Generación de API desde contrato OpenAPI

- **Decision**: Plugin Gradle `org.openapi.generator` (openapi-generator-gradle-plugin), generador
  `spring`, configurado en modo **interfaces + modelos únicamente**
  (`interfaceOnly=true`, `useSpringBoot3=true`, `library=spring`), apuntando al contrato en
  `src/main/resources/openapi/citasalud-api.yaml`. El código generado se ubica en un paquete
  dedicado (`org.ups.citasalud.infrastructure.web.generated`) y se regenera en cada build
  (`compileJava` depende de la tarea `openApiGenerate`); no se versiona en git como código fuente
  editable.
- **Rationale**: Satisface el Principio IV (API First, NON-NEGOTIABLE) de forma literal: contrato
  primero, generación obligatoria vía openapi-generator, aislado en Interface Adapters. El modo
  "interfaces + DTOs" (sin generar controllers concretos) permite que los `@RestController` reales
  vivan en `infrastructure/web` e implementen las interfaces generadas, cumpliendo también la
  Dependency Rule (Principio I): el dominio nunca importa clases generadas.
- **Alternatives considered**:
  - *Generar controllers completos*: más rápido para prototipos, pero mezclaría lógica de negocio
    con código regenerable; descartado por violar la separación de capas.
  - *Escribir DTOs y contratos a mano*: más control, pero contradice directamente el Principio IV
    ("uso de openapi-generator... es OBLIGATORIO").

## 3. Cobertura de pruebas con JaCoCo

- **Decision**: Plugin `jacoco` de Gradle, con `jacocoTestCoverageVerification` configurado con dos
  reglas: `element = CLASS` con `minimum = 0.80` (estrictamente mayor que 80% se aproxima
  configurando el umbral en 0.801 o validando con `> 0.80` en la regla) y `element = BUNDLE` con
  `minimum = 0.80` para cobertura global. El paquete
  `org.ups.citasalud.infrastructure.web.generated` (código generado por openapi-generator) se
  excluye explícitamente de las reglas de cobertura, documentando la exclusión en `build.gradle`
  como exige el Principio V. La tarea `check` de Gradle depende de
  `jacocoTestCoverageVerification`, de modo que el build falla si no se cumplen los umbrales
  (Puertas Obligatorias de la constitution).
- **Rationale**: Cumple el Principio V de forma directa y hace el gate ejecutable tanto en local
  (`./gradlew check`) como en CI, tal como exige la sección "Puertas Obligatorias" de la
  constitution.
- **Alternatives considered**: Herramientas de cobertura alternativas (JCov, Cobertura) —
  descartadas porque el Principio V nombra JaCoCo explícitamente como obligatorio.

## 4. Autenticación del paciente (FR-005)

- **Decision**: Se agrega `spring-boot-starter-security` con autenticación basada en sesión
  (cookie de sesión HTTP estándar de Spring Security), configurada para exigir un usuario
  autenticado en los endpoints de consulta/selección/confirmación de citas de esta historia. La
  historia **no** implementa el flujo de registro/alta de cuentas ni la pantalla de login en sí
  (fuera de alcance, ver Assumptions del spec); se implementa únicamente el mecanismo de
  verificación de sesión y un proveedor de autenticación mínimo/reutilizable que permita probar el
  flujo end-to-end. Si ya existe o se planea un servicio de identidad separado, este mecanismo debe
  reemplazarse por esa integración sin tocar `domain`/`application` (aislado en
  `infrastructure/security`). `PacienteAutenticadoPort` MUST exponer no solo el `id` del paciente
  autenticado, sino también `nombreCompleto`, `documentoIdentificacion` y `numeroWhatsApp` de su
  perfil — son necesarios para FR-006/FR-011/FR-014 (registrar la cita y enviar la confirmación al
  número correcto) y ningún otro Port de esta historia los provee.
- **Rationale**: Sesión HTTP es el patrón estándar por defecto para una aplicación Spring Boot MVC
  monolítica como esta (no hay SPA/cliente móvil descrito en el spec), consistente con la guía de
  "usar defaults razonables" cuando no se especifica el método. Aislar la configuración en
  `infrastructure/security` respeta la Dependency Rule: los Use Cases dependen de un
  `PacienteAutenticadoPort` (o mecanismo equivalente para obtener el paciente autenticado actual),
  no de Spring Security directamente.
- **Alternatives considered**:
  - *OAuth2/JWT*: más apropiado si hubiera un cliente SPA/móvil separado, pero no hay evidencia de
    eso en el spec ni en el stack actual (`spring-boot-starter-webmvc`, plantillas Thymeleaf-like
    en `src/main/resources/templates`); se descarta por sobre-ingeniería (YAGNI) mientras no se
    confirme lo contrario.
  - **Riesgo documentado**: esta historia asume que existe (o se construye en paralelo) un
    mecanismo real de creación de cuentas/credenciales. Si esa historia no existe aún, US-01 no es
    verificable end-to-end sin un "seed" de cuentas de prueba — se documenta como dependencia en
    `plan.md` (Complexity Tracking / Summary), no se inventa esa historia aquí.

## 5. Bloqueo temporal de franjas (concurrencia, FR-009/FR-010)

- **Decision**: El bloqueo temporal se modela como un atributo de la propia fila de `franja_horaria`
  en la base de datos relacional (`bloqueada_por_paciente_id`, `bloqueada_hasta`), actualizado con
  una escritura condicional (`UPDATE ... WHERE estado = 'DISPONIBLE'`, verificando filas afectadas)
  para garantizar atomicidad sin necesidad de infraestructura de locking distribuido adicional. La
  confirmación de la cita (FR-011) también se hace con una escritura condicional equivalente
  (`UPDATE ... WHERE bloqueada_por_paciente_id = :paciente AND bloqueada_hasta > now()`), y la
  columna `franja_horaria(medico_id, fecha_hora)` lleva una restricción de unicidad para que una
  franja ocupada nunca pueda asociarse a dos citas, incluso ante una condición de carrera.
- **Rationale**: Con el volumen y escala de esta historia (un único servicio Spring Boot con una
  base de datos relacional, sin mención de despliegue multi-instancia en el spec), una escritura
  condicional a nivel de base de datos es suficiente para cumplir SC-003 (0% de dobles reservas) sin
  introducir un almacén de locks distribuido (p. ej. Redis) que la historia no menciona ni justifica
  — decisión alineada con YAGNI (Principio III).
- **Alternatives considered**:
  - *Lock distribuido (Redis/Redisson)*: resolvería concurrencia también en despliegues
    multi-instancia, pero no hay evidencia en el spec ni en el stack actual de que el servicio se
    despliegue así; se descarta por ahora y queda como extensión futura si cambia el contexto de
    despliegue.
  - *Lock pesimista de base de datos (`SELECT ... FOR UPDATE`)*: alternativa válida, pero la
    escritura condicional (`UPDATE ... WHERE`) logra el mismo resultado con menor tiempo de
    contención y es más simple de testear en H2/PostgreSQL por igual.

## 6. Liberación automática del bloqueo tras 5 minutos (FR-010)

- **Decision**: No se libera mediante un job/scheduler en segundo plano; se libera de forma
  **perezosa** (lazy): cualquier lectura o intento de confirmación sobre una franja evalúa si
  `bloqueada_hasta` ya expiró y, si es así, la trata como disponible (y opcionalmente limpia el
  estado en esa misma operación). Se complementa con un job programado (`@Scheduled`) de barrido
  periódico (p. ej. cada minuto) que limpia bloqueos vencidos, solo como housekeeping, no como
  mecanismo del que dependa la corrección funcional.
- **Rationale**: La corrección funcional (que una franja vencida no bloquee a otros pacientes) no
  debe depender de la cadencia de un job en segundo plano; evaluarlo en el momento de la consulta
  garantiza el comportamiento correcto incluso si el barrido aún no ha corrido. El job periódico es
  solo una optimización de limpieza, consistente con YAGNI (no se requiere infraestructura de colas
  o temporizadores distribuidos para esta historia).
- **Alternatives considered**: *Expiración mediante cola de mensajería con retraso* — descartada
  por sobre-ingeniería para el alcance de esta historia (no hay mención de mensajería en el spec ni
  en el stack).

## 7. Envío y reintentos de confirmación por WhatsApp (FR-014/FR-016/FR-017)

- **Decision**: Se define un `Port` de dominio/aplicación (`NotificacionCitaPort`) con una única
  operación de envío, implementado por un `Adapter` en `infrastructure/notification` que encapsula
  la llamada HTTP a un proveedor de WhatsApp Business API (usando `RestClient` de Spring, ya
  disponible transitivamente vía `spring-boot-starter-webmvc`). El reintento automático (FR-016) se
  implementa con `@Retryable` (Spring Retry) sobre el adapter, con un número de intentos y backoff
  configurables por propiedades (`app.notificaciones.whatsapp.reintentos`,
  `...backoff-ms`), **sin fijar aquí un valor concreto** — coherente con el
  `[NEEDS CLARIFICATION: número de reintentos de envío]` que el spec deja abierto
  intencionalmente. El proveedor concreto (credenciales, URL base) también quedan fuera del alcance
  de esta historia y se inyectan por configuración externa.
- **Rationale**: Aislar el envío detrás de un Port cumple la Dependency Rule (el `application` no
  conoce el proveedor de WhatsApp concreto) y permite que el número de reintentos se resuelva en
  configuración/`application.yml` durante la implementación, sin inventar un valor de negocio no
  solicitado en el spec.
- **Alternatives considered**: *Cola de mensajería + worker separado* para los reintentos —
  descartada por sobre-ingeniería (YAGNI) dado que no hay volumen ni SLA de mensajería mencionados
  en el spec; `@Retryable` en el propio adapter es suficiente para el alcance actual.

## Resumen de dependencias nuevas a incorporar en `build.gradle`

| Dependencia | Motivo |
|---|---|
| `org.openapitools:openapi-generator-gradle-plugin` | Principio IV (API First obligatorio) |
| `jacoco` (plugin nativo de Gradle) | Principio V (cobertura obligatoria) |
| `org.springframework.boot:spring-boot-starter-security` | FR-005 (exigir sesión autenticada) |
| `org.springframework.retry:spring-retry` + `spring-boot-starter-aop` | FR-016 (reintentos de envío) |
| `org.springframework.boot:spring-boot-starter-test` (ya vía starters existentes) | Principio II (JUnit 5, Mockito, AssertJ) |

**Output**: todas las incógnitas del Technical Context quedan resueltas; no quedan
`NEEDS CLARIFICATION` pendientes para Phase 1.
