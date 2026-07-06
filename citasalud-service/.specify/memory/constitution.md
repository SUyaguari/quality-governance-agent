<!--
Sync Impact Report
===================
Version change: 1.0.0 → 1.1.0

Principios: sin cambios de fondo (I–V se mantienen); se referencian de forma más
explícita desde el flujo de desarrollo y las puertas obligatorias.

Secciones modificadas:
  - "Flujo de Desarrollo y Quality Gates" → dividida y reescrita como:
      - "Flujo de Desarrollo (secuencia obligatoria)": ahora es una secuencia
        numerada y ordenada (contrato OpenAPI → generación con openapi-generator →
        ubicación en domain/application/infrastructure respetando la Dependency
        Rule → pruebas BDD en rojo → implementación SOLID/YAGNI/DRY en verde →
        ejecución de pruebas + verificación JaCoCo). Antes era una lista de gates
        sin orden explícito.
      - "Puertas Obligatorias (Quality Gates)": sección nueva y explícita con las
        puertas bloqueantes: build MUST compilar, tests + JaCoCo MUST pasar en
        local y en CI, todo PR MUST verificar los cinco principios, violaciones
        MUST justificarse en la tabla Complexity Tracking del plan o corregirse,
        código generado MUST permanecer sin ediciones manuales.
  - "Governance" → el párrafo de "Cumplimiento" se ajustó para remitir a la nueva
    sección de Puertas Obligatorias en lugar de duplicar su contenido.

Secciones añadidas: "Puertas Obligatorias (Quality Gates)"
Secciones eliminadas: ninguna (la sección anterior se dividió, no se eliminó)

Templates revisados:
  - .specify/templates/plan-template.md → ✅ Alineado (el gate "Constitution Check"
    y la tabla "Complexity Tracking" ya existen y son el mecanismo de justificación
    de violaciones referenciado por las nuevas Puertas Obligatorias)
  - .specify/templates/spec-template.md → ✅ Alineado (sin cambios necesarios)
  - .specify/templates/tasks-template.md → ✅ Alineado (ya refleja tests
    obligatorios y contrato OpenAPI como prerequisito; consistente con la nueva
    secuencia ordenada)
  - .specify/templates/checklist-template.md → ✅ Alineado (genérico, sin cambios)

Deferred TODOs: ninguno
-->

# citasalud-service Constitution

## Core Principles

### I. Clean Architecture (Arquitectura Limpia)

El proyecto MUST implementarse siguiendo la Clean Architecture definida por Robert C.
Martin ("Uncle Bob"). Se reconocen explícitamente las cuatro capas concéntricas —
**Entities** (reglas de negocio empresariales), **Use Cases** (reglas de negocio de la
aplicación), **Interface Adapters** (controllers, presenters, gateways, mappers) y
**Frameworks & Drivers** (Spring, JPA, Web, DB, herramientas externas). La **Dependency
Rule** es NON-NEGOTIABLE: el código fuente de una capa interna NUNCA MUST depender de
una capa externa; toda dependencia hacia afuera MUST invertirse mediante interfaces
(Ports) definidas en la capa interna e implementadas por Adapters en la capa externa.
Las Entities y Use Cases MUST permanecer libres de anotaciones y tipos específicos de
framework (Spring, JPA, Jakarta Validation, etc.); esas dependencias solo MUST aparecer
en Interface Adapters y Frameworks & Drivers.

**Rationale**: separar las reglas de negocio de los detalles de infraestructura permite
que el dominio sea testeable de forma aislada, que el framework (Spring Boot) sea
reemplazable sin reescribir la lógica de negocio, y evita que cambios en la base de
datos o en la capa web se propaguen hacia el core del sistema.

### II. Testing Basado en BDD (Behavior-Driven Development)

Todo comportamiento observable MUST estar cubierto por pruebas escritas bajo el enfoque
BDD (estructura Given-When-Then / Arrange-Act-Assert con nomenclatura orientada a
comportamiento, no a implementación). MUST existir cobertura en los tres niveles:

- **Pruebas unitarias**: validan una Entity, Use Case o componente aislado, con sus
  dependencias externas (Ports) dobladas (mocks/stubs).
- **Pruebas de integración**: validan la colaboración real entre Adapters y
  Frameworks & Drivers (por ejemplo, repositorios JPA contra una base de datos real o
  en memoria, serialización de DTOs, configuración de Spring).
- **Pruebas funcionales (end-to-end)**: validan un flujo completo de negocio a través
  de la API expuesta, verificando el comportamiento desde la perspectiva del
  consumidor del contrato OpenAPI.

Las pruebas MUST escribirse antes o junto con el código que las satisface, nunca como
una tarea diferida al final del desarrollo de una historia de usuario.

**Rationale**: BDD alinea el lenguaje de las pruebas con el comportamiento esperado por
el negocio, hace explícitos los escenarios de aceptación y reduce la ambigüedad entre
lo que el código hace y lo que debería hacer.

### III. Buenas Prácticas de Diseño (SOLID, YAGNI, DRY)

Todo código de producción MUST cumplir los principios SOLID (Single Responsibility,
Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion). El
diseño MUST aplicar YAGNI ("You Aren't Gonna Need It"): SHOULD NOT introducirse
abstracciones, configuraciones o capas de flexibilidad para requisitos hipotéticos no
solicitados. El código MUST aplicar DRY ("Don't Repeat Yourself"): la duplicación de
lógica de negocio o de reglas de validación MUST eliminarse mediante una única fuente
de verdad, evitando a la vez abstracciones prematuras que violen YAGNI.

**Rationale**: estos principios son el fundamento técnico que sostiene la Clean
Architecture; sin SOLID las fronteras entre capas se degradan, sin YAGNI el sistema
acumula complejidad innecesaria, y sin DRY los defectos se multiplican al no tener una
única fuente de verdad.

### IV. API First con Contrato OpenAPI (NON-NEGOTIABLE)

Toda API MUST diseñarse bajo el enfoque **API First**: el contrato OpenAPI (formato
YAML) MUST redactarse y aprobarse ANTES de escribir cualquier código de
implementación. El uso de **openapi-generator** para generar los endpoints (interfaces
de controllers), DTOs y modelos a partir del contrato es OBLIGATORIO y MUST integrarse
como parte del build (Gradle). El código generado MUST tratarse como artefacto
derivado: NUNCA MUST editarse manualmente, y MUST regenerarse en cada build a partir del
contrato versionado en el repositorio.

La generación MUST aislarse en la capa de Interface Adapters / Frameworks & Drivers: los
Use Cases y Entities NUNCA MUST depender directamente de los DTOs o interfaces
generadas por openapi-generator. Los Adapters (controllers) MUST implementar las
interfaces generadas y MUST mapear entre los DTOs generados y los modelos de dominio,
de modo que cambios en el contrato OpenAPI no afecten la lógica de negocio del core.

**Rationale**: fijar el contrato antes de la implementación evita divergencias entre lo
documentado y lo implementado, permite a consumidores de la API trabajar en paralelo
contra un mock/contrato estable, y automatizar la generación de boilerplate reduce
errores manuales y mantiene el contrato como única fuente de verdad de la API.

### V. Calidad y Cobertura de Pruebas (JaCoCo)

**JaCoCo** MUST usarse como herramienta obligatoria para generar los reportes de
cobertura de pruebas, integrado en el pipeline de build (Gradle). MUST cumplirse, como
mínimo:

- Cobertura por clase (`class coverage`) MUST ser superior al 80% (> 80%).
- Cobertura global del proyecto (`overall coverage`) MUST ser mayor o igual al 80%
  (>= 80%).

El build MUST fallar (quality gate bloqueante) si alguna de estas métricas no se
cumple. Las clases o paquetes generados automáticamente por openapi-generator (ver
Principio IV) PUEDEN excluirse del cálculo de cobertura por no contener lógica de
negocio propia, siempre que dicha exclusión esté explícitamente configurada y
documentada en el build de JaCoCo.

**Rationale**: un umbral de cobertura verificable automáticamente convierte el
Principio II (BDD testing) en una garantía medible y no solo en una intención,
evitando regresiones silenciosas en el dominio y en los casos de uso.

## Restricciones Tecnológicas y de Stack

- **Lenguaje/Runtime**: Java (toolchain versión 25) sobre Spring Boot.
- **Build tool**: Gradle MUST ser la herramienta de build; los plugins de JaCoCo y de
  generación OpenAPI (openapi-generator) MUST configurarse en `build.gradle`.
- **Persistencia**: Spring Data JPA; el acceso a datos MUST implementarse como Adapter
  (Repository) detrás de un Port definido en la capa de Use Cases, nunca invocado
  directamente desde Entities o Use Cases.
- **Lombok**: PUEDE usarse únicamente en Interface Adapters y Frameworks & Drivers
  (DTOs, entities JPA); SHOULD NOT usarse en Entities/Use Cases del dominio para no
  acoplar el core a una dependencia de framework.
- **Contrato de API**: MUST residir en el repositorio como archivo(s) OpenAPI
  versionado(s), tratados como fuente de verdad del Principio IV.

## Flujo de Desarrollo (secuencia obligatoria)

Toda funcionalidad que exponga o modifique una API MUST desarrollarse siguiendo esta
secuencia, en este orden. No MUST saltarse ni reordenarse ningún paso:

1. **Contrato OpenAPI primero**: MUST crearse o actualizarse el contrato OpenAPI
   (Principio IV) y MUST revisarse/aprobarse antes de escribir cualquier código de
   implementación.
2. **Generación con openapi-generator**: a partir del contrato aprobado, MUST
   generarse el código (interfaces de controllers, DTOs, modelos) mediante
   `openapi-generator` integrado en el build de Gradle (Principio IV).
3. **Ubicación en capas de Clean Architecture**: el código propio (no generado) MUST
   ubicarse explícitamente en `domain` (Entities/Use Cases), `application` (Use
   Cases/orquestación) e `infrastructure` (Interface Adapters, Frameworks & Drivers),
   respetando la Dependency Rule (Principio I) desde el diseño inicial, no como
   corrección posterior.
4. **Pruebas BDD primero (rojo)**: MUST escribirse las pruebas unitarias, de
   integración y/o funcionales bajo el enfoque BDD (Principio II) correspondientes al
   comportamiento esperado, y MUST verificarse que fallan antes de implementar la
   lógica que las satisface.
5. **Implementación SOLID/YAGNI/DRY (verde)**: MUST implementarse la lógica de
   negocio aplicando SOLID, YAGNI y DRY (Principio III) hasta que las pruebas del
   paso 4 pasen.
6. **Ejecución de pruebas y verificación JaCoCo**: MUST ejecutarse la suite completa
   de pruebas y MUST generarse el reporte de JaCoCo, confirmando el cumplimiento de
   los umbrales de cobertura (Principio V) antes de considerar la tarea completa.

## Puertas Obligatorias (Quality Gates)

Estas puertas son bloqueantes y MUST cumplirse tanto en el entorno local del
desarrollador como en el pipeline de CI antes de fusionar un Pull Request:

- **Compilación**: el proyecto MUST compilar sin errores (`gradle build`) en local y
  en CI. Un build roto NUNCA MUST fusionarse.
- **Pruebas**: la suite completa de pruebas unitarias, de integración y funcionales
  (Principio II) MUST pasar en local y MUST volver a pasar en CI.
- **Cobertura JaCoCo**: los umbrales del Principio V (> 80% por clase, >= 80%
  global) MUST verificarse con JaCoCo tanto en local como en CI; el pipeline de CI
  MUST bloquear el merge si no se cumplen.
- **Verificación de los cinco principios**: todo Pull Request MUST incluir una
  verificación explícita de cumplimiento de los cinco principios de esta
  constitution (Clean Architecture, BDD Testing, SOLID/YAGNI/DRY, API First,
  Cobertura JaCoCo), replicando el gate "Constitution Check" del `plan.md`
  correspondiente.
- **Justificación de violaciones**: cualquier violación a un principio MUST
  registrarse y justificarse explícitamente en la tabla "Complexity Tracking" del
  `plan.md` de la feature, o MUST corregirse antes de fusionar. No se permite
  fusionar una violación no documentada ni sin corregir.
- **Código generado inmutable**: el código producido por `openapi-generator` MUST
  permanecer sin ediciones manuales; cualquier diferencia entre el contrato OpenAPI
  y el código generado MUST resolverse regenerando desde el contrato, nunca editando
  el artefacto generado a mano.

## Governance

Esta constitution prevalece sobre cualquier otra práctica, guía o convención del
proyecto. En caso de conflicto entre esta constitution y cualquier otro documento
(incluido `CLAUDE.md`), la constitution MUST prevalecer, y `CLAUDE.md` MUST usarse como
guía operativa de desarrollo en tiempo de ejecución consistente con estos principios.

**Enmiendas**: cualquier cambio a esta constitution MUST proponerse mediante un Pull
Request que modifique este archivo, documentando explícitamente el motivo del cambio y
su impacto en los templates dependientes (`plan-template.md`, `spec-template.md`,
`tasks-template.md`). El PR MUST ser aprobado antes de hacer merge.

**Versionado**: esta constitution sigue versionado semántico (MAJOR.MINOR.PATCH):
- **MAJOR**: eliminación o redefinición incompatible de un principio existente.
- **MINOR**: adición de un nuevo principio o sección, o expansión material de guía
  existente.
- **PATCH**: aclaraciones de redacción, correcciones tipográficas o refinamientos no
  semánticos.

**Cumplimiento**: la verificación de cumplimiento de los cinco principios, la
justificación de violaciones y las puertas bloqueantes de compilación/pruebas/JaCoCo
MUST aplicarse tal como se definen en la sección "Puertas Obligatorias (Quality
Gates)". Todo `plan.md` generado por `/speckit-plan` MUST incluir el gate
"Constitution Check" resuelto contra esta constitution.

**Version**: 1.1.0 | **Ratified**: 2026-07-02 | **Last Amended**: 2026-07-02
