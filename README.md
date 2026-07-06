# Quality Governance Agent Workspace

Repositorio de trabajo para una práctica de maestría en Ingeniería de Software. La raíz no contiene una sola aplicación: agrupa un agente de aseguramiento de calidad y un backend Spring Boot que sirve como caso de estudio.

## Qué hay en este workspace

### `quality-agent/`

Agente de calidad y gobierno para proyectos Spring Boot ya implementados.

Su objetivo es verificar tres pilares antes de aprobar un cambio:

- pruebas y cobertura
- seguridad
- cumplimiento de requisitos trazados desde `spec.md`

Incluye:

- comandos de Claude Code para verificación y generación de reportes
- un gate determinista en Python que bloquea si el `verification.json` no cumple los umbrales
- integración con Semgrep MCP para el pilar de seguridad
- un ejemplo pequeño llamado `examples/citasalud-agenda`

Documentación principal: [quality-agent/README.md](/C:/Users/cebol/OneDrive/Desktop/Sebasss/Maestria/quality-governance-agent/quality-agent/README.md)

### `citasalud-service/`

Servicio backend Spring Boot que implementa la historia de negocio "reserva de cita en línea 24/7".

El proyecto usa:

- Java 25
- Spring Boot 4
- Gradle
- OpenAPI Generator
- Spring Security
- JPA con H2
- pruebas unitarias, de integración y funcionales

Su código está organizado con enfoque de Clean Architecture:

- `domain/`
- `application/`
- `infrastructure/`

También incluye artefactos de Spec Kit en `specs/` y evidencia de calidad en `quality-output/`.

## Relación entre ambos proyectos

La idea del workspace es esta:

1. `citasalud-service/` representa el producto que se construyó.
2. `quality-agent/` representa el agente que lo audita.
3. `quality-agent/examples/citasalud-agenda/` es un ejemplo pequeño y deliberadamente imperfecto para demostrar un bloqueo del gate.

En otras palabras, este repositorio mezcla el "auditor" y un "auditado".

## Estructura rápida

```text
quality-governance-agent/
├── README.md
├── quality-agent/
│   ├── .claude/
│   ├── .mcp.json
│   ├── examples/
│   │   └── citasalud-agenda/
│   ├── CLAUDE.md
│   └── README.md
└── citasalud-service/
    ├── .claude/
    ├── specs/
    ├── src/
    ├── quality-output/
    ├── build.gradle
    └── settings.gradle
```

## Por dónde empezar

### Si quieres entender el agente

Revisa primero:

- [quality-agent/README.md](/C:/Users/cebol/OneDrive/Desktop/Sebasss/Maestria/quality-governance-agent/quality-agent/README.md)
- [quality-agent/CLAUDE.md](/C:/Users/cebol/OneDrive/Desktop/Sebasss/Maestria/quality-governance-agent/quality-agent/CLAUDE.md)
- [quality-agent/.claude/hooks/quality-gate.py](/C:/Users/cebol/OneDrive/Desktop/Sebasss/Maestria/quality-governance-agent/quality-agent/.claude/hooks/quality-gate.py)

### Si quieres entender el backend de citas

Revisa primero:

- [citasalud-service/specs/001-reserva-cita-online-24-7/spec.md](/C:/Users/cebol/OneDrive/Desktop/Sebasss/Maestria/quality-governance-agent/citasalud-service/specs/001-reserva-cita-online-24-7/spec.md)
- [citasalud-service/specs/001-reserva-cita-online-24-7/plan.md](/C:/Users/cebol/OneDrive/Desktop/Sebasss/Maestria/quality-governance-agent/citasalud-service/specs/001-reserva-cita-online-24-7/plan.md)
- [citasalud-service/src/main/java/org/ups/citasalud/application/usecase/ConfirmarCitaUseCase.java](/C:/Users/cebol/OneDrive/Desktop/Sebasss/Maestria/quality-governance-agent/citasalud-service/src/main/java/org/ups/citasalud/application/usecase/ConfirmarCitaUseCase.java)
- [citasalud-service/src/main/java/org/ups/citasalud/infrastructure/security/SecurityConfig.java](/C:/Users/cebol/OneDrive/Desktop/Sebasss/Maestria/quality-governance-agent/citasalud-service/src/main/java/org/ups/citasalud/infrastructure/security/SecurityConfig.java)

## Requisitos

- Git
- Python 3 para el gate y el generador de reportes del agente
- JDK 25 para `citasalud-service/`
- JDK 21 si quieres ejecutar el ejemplo `quality-agent/examples/citasalud-agenda/`
- Gradle Wrapper incluido en los proyectos Java

## Comandos útiles

### Backend `citasalud-service`

```bash
cd citasalud-service
./gradlew test
./gradlew jacocoTestReport
./gradlew bootRun
```

### Ejemplo pequeño `citasalud-agenda`

```bash
cd quality-agent/examples/citasalud-agenda
./gradlew test
./gradlew jacocoTestReport
```

### Agente de calidad

Desde `quality-agent/`, el ejemplo documentado es:

```bash
cd quality-agent
claude
/quality:verify examples/citasalud-agenda
```

Para auditar el proyecto hermano `citasalud-service/` desde esa misma carpeta:

```bash
/quality:verify ../citasalud-service
/quality:generate-report ../citasalud-service
```

### Ejecutar el gate manualmente

```bash
python3 quality-agent/.claude/hooks/quality-gate.py citasalud-service/quality-output/verification.json
python3 quality-agent/.claude/scripts/build-report.py citasalud-service/quality-output/verification.json
```

## Observaciones de diseño

- `quality-agent/` está pensado como herramienta de validación, no como aplicación de negocio.
- `citasalud-service/` es el proyecto más completo del dominio y ya contiene una implementación real del flujo de citas.
- El README raíz debe leerse como mapa del workspace; el detalle operativo vive dentro de cada subproyecto.
