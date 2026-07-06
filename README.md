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
|-- README.md
|-- quality-agent/
|   |-- .claude/
|   |-- .mcp.json
|   |-- examples/
|   |   `-- citasalud-agenda/
|   |-- CLAUDE.md
|   `-- README.md
`-- citasalud-service/
    |-- .claude/
    |-- specs/
    |-- src/
    |-- quality-output/
    |-- build.gradle
    `-- settings.gradle
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

---

## Demostración del gate

Para demostrar el gate de Definition of Done se ejecutó una verificación sobre `citasalud-service` en la que el proyecto quedó bloqueado por un criterio no satisfecho. El agente detectó que el requisito `FR-005` seguía marcado como `incumple`, por lo que el veredicto fue `BLOQUEADO` aunque los pilares de pruebas y seguridad ya estaban en verde.

La evidencia del bloqueo y su análisis es la siguiente:

1. `Bloqueo del gate en /quality:verify`  
El comando `/quality:verify` ejecutado desde `quality-agent` devolvió un bloqueo por criterio pendiente.

![Bloqueo del gate en quality verify](<evidencia/Prueba 1 bloqueo gate.png>)

2. `Reporte bloqueado por criterio pendiente`  
El reporte visual `report.html` mostró el mismo estado `BLOQUEADO`, con fallo en el pilar de criterios y el requisito `FR-005` como pendiente.

![Reporte bloqueado por FR-005](<evidencia/Prueba 2 bloqueo gate-reporte.png>)

3. `Revisión del verification.json para identificar la causa`  
Luego se revisó el `verification.json` del proyecto para identificar por qué el criterio no cumplía y qué evidencia faltaba en las pruebas.

![Revisión inicial del verification json](<evidencia/Prueba 3 solucion bloqueo bycoding.png>)

4. `Análisis del criterio FR-005 y propuesta de corrección`  
A partir de esa revisión se confirmó que el problema no estaba en la regla de seguridad en sí, sino en la ausencia de pruebas automatizadas que demostraran el rechazo sin sesión en los endpoints requeridos por `FR-005`.

![Análisis del criterio FR-005](<evidencia/Prueba 4 solucion bloqueo bycoding propuesta por .png>)

5. `Aplicación de la solución y cierre del criterio`  
Después se aplicó la corrección agregando las pruebas faltantes y actualizando la evidencia de verificación, con lo que el criterio pasó de `incumple` a `cumple`.

![Aplicación de la solución al criterio FR-005](<evidencia/Prueba 5 solucion bloqueo bycoding propuesta por claude.png>)

Una vez corregido el criterio, se repitió la verificación completa del proyecto y el gate pasó sin bloqueos:

6. `Verificación aprobada por el gate`  
La nueva ejecución de `/quality:verify` devolvió el veredicto `APROBADO`, indicando que los tres pilares quedaron conformes.

![Gate aprobado tras corregir FR-005](<evidencia/Prueba 6 Aprobación del gate.png>)

7. `Reporte final en estado aprobado`  
El reporte visual final también quedó en estado `APROBADO`, confirmando la consistencia entre `verification.json`, gate y `report.html`.

![Reporte final aprobado](<evidencia/Prueba 7 Aprobación del reporte.png>)

En conjunto, esta secuencia muestra el comportamiento esperado del agente: primero bloquea cuando un criterio del `spec.md` no tiene respaldo suficiente en pruebas, y luego permite el avance una vez que la evidencia fue completada y la verificación se ejecutó nuevamente con resultado satisfactorio.

---

## Reflexión breve

La percepción de terminado cambió debido a que ya no basta con observar los avances y verificar el funcionamiento desde nuestro criterio, es decir, consideraba que el flujo o la funcionalidad estaba correcta si el código compilaba, mientras que con el gate determinista, el cierre real ocurre cuando los tres pilares quedan en verde: que todas las pruebas pasen y la cobertura cumpla el umbral determinado,  no existan vulnerabilidades criticas ni secretos expuestos y que cada requisito del spec.md esté respaldado con evidencia real. En este sentido el gate obligó a validar que no solo el código funcione, sino también que esté demostrado y documentado.

El pilar que más me costó dejar en verde fue el de criterios, pruebas y seguridad podían medirse de forma directa con la ejecución de la suite test, cobertura y escaneo automático, pero los criterios exigieron comprobar que cada `FR-xxx` del `spec.md` tuviera evidencia en pruebas reales. El caso de `FR-005` mostro justamente ese dilema: la protección existía en el código, pero no había una prueba dedicada que la demostrara para los endpoints, por lo que el criterio no podía percibirse como cumplido.

Además, el pilar de seguridad también presentó un problema, aunque pudo quedar en verde según las reglas del gate, hubo observaciones en la seguridad, en el reporte hubo hallazgos de triage, como usuarios de prueba en memoria y la consola H2 se encuentra habilitada para desarrollo, es decir es un problema de seguridad y al consultar como solventar el problema, Claude Code me recomendó no solucionar si me encontraba en un ambiente de pruebas y documentarlo como deuda técnica para resolverlo en un futuro, pero con lo visto en clases la mejor decisión fue resolver la deuda técnica inmediatamente.

Por último, un gate de Definition of Done sirve para estandarizar la calidad mínima antes de aceptar cambios, reducir discusiones en revisiones y evitar que se integren funcionalidades con vacíos de prueba o requisitos mal cubiertos. A la vez, el escaneo automático de seguridad vía MCP aporta la revisión repetible y temprana sobre vulnerabilidades y secretos expuestos, lo que ayuda bastante a detectar riesgos antes de llegar a ambientes más costosos (Recordando la clase de más temprano, menos costo). En conclusión, ambos mecanismos funcionan como una barrera preventiva que mejora la confianza del equipo en la entrega y hace más consistente la definición de "Terminado".
