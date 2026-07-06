# Feature Specification: Reserva de cita en línea 24/7

**Feature Branch**: `001-reserva-cita-online-24-7`

**Created**: 2026-07-02

**Status**: Draft

**Input**: User description: "US-01 · Reserva de cita en línea 24/7 · épica E-01 · 8 pts. Como paciente, quiero
reservar una cita en línea en cualquier momento del día, para no tener que llamar durante mi horario de
almuerzo ni acumular intentos fallidos.

Criterios de aceptación (Gherkin):
- Dado que el paciente accede al sistema fuera del horario de atención telefónica, cuando elige médico, fecha
  y hora disponibles y confirma, entonces la cita queda registrada y el paciente recibe confirmación por
  WhatsApp.
- Dado que el paciente intenta seleccionar una franja ya ocupada, cuando intenta confirmarla, entonces el
  sistema la muestra como no disponible y lo invita a elegir otra franja."

**Épica**: E-01 · **Estimación**: 8 pts

**Decisiones de alcance confirmadas** (no estaban en la historia original; se documentan aquí para que no
vuelvan a interpretarse de forma distinta):

1. El paciente **MUST tener una cuenta previamente creada** en el sistema y **MUST iniciar sesión (login)**
   antes de poder reservar. La creación/registro de esa cuenta y la gestión de credenciales son una historia
   distinta, fuera de alcance de US-01 (decisión confirmada el 2026-07-02).
2. El número de WhatsApp al que se envía la confirmación es el que **ya está registrado en el perfil de la
   cuenta del paciente autenticado**; no se vuelve a solicitar durante el flujo de reserva (decisión
   confirmada el 2026-07-02).
3. Para evitar que dos pacientes confirmen la misma franja libre al mismo tiempo, el sistema **bloquea
   temporalmente la franja** apenas el paciente la selecciona, mientras completa el resto del flujo, por
   **5 minutos** (decisión confirmada el 2026-07-02).
4. Un mismo paciente **MUST NOT tener más de 2 citas activas** registradas al mismo tiempo (decisión
   confirmada el 2026-07-02).

## Clarifications

### Session 2026-07-02

- Q: ¿Cómo se identifica el paciente para reservar — ingresa sus datos en el mismo flujo (sin cuenta previa)
  o requiere una cuenta/login previamente creado? → A: Requiere cuenta previamente creada; el paciente MUST
  iniciar sesión antes de reservar. El registro/alta de la cuenta es una historia distinta, fuera de alcance
  de US-01.
- Q: Si falla el envío de la confirmación por WhatsApp, ¿qué debe hacer el sistema con la cita? → A: La cita
  permanece registrada; el sistema MUST reintentar el envío automáticamente un número de veces antes de
  marcarlo como fallido (el número exacto de reintentos queda como parámetro pendiente de definir, ver
  Assumptions).
- Q: ¿Cuánto tiempo debe permanecer bloqueada una franja antes de liberarse automáticamente si el paciente
  no confirma? → A: 5 minutos.
- Q: ¿Existe algún límite de citas activas que un mismo paciente pueda tener simultáneamente? → A: Sí,
  máximo 2 citas activas por paciente.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reservar una cita médica en línea sin depender del horario telefónico (Priority: P1)

Como paciente, quiero reservar una cita en línea en cualquier momento del día, para no tener que llamar
durante mi horario de almuerzo ni acumular intentos fallidos.

**Why this priority**: Es la única historia de esta épica y resuelve directamente el dolor del paciente (no
poder llamar en horario laboral) permitiéndole autogestionar su cita sin intervención humana. Sin esta
capacidad no existe producto que entregar.

**Independent Test**: Puede probarse de forma completa ingresando al sistema fuera del horario de atención
telefónica, iniciando sesión con una cuenta de paciente ya existente, seleccionando médico, fecha y hora
disponibles, y confirmando la cita, y verificando que (a) la cita queda registrada y (b) se envía la
confirmación al número de WhatsApp registrado en el perfil del paciente — sin que ninguna persona del centro
médico intervenga en el proceso.

**Flujo principal (camino feliz)**:

1. El paciente accede al sistema, en cualquier momento del día (dentro o fuera del horario de atención
   telefónica), e inicia sesión con su cuenta ya existente.
2. El sistema muestra los médicos disponibles y, por cada médico, las franjas horarias (fecha y hora) que
   están actualmente disponibles.
3. El paciente selecciona un médico y una franja horaria disponible.
4. El sistema bloquea temporalmente esa franja para este paciente, de modo que ningún otro paciente pueda
   seleccionarla mientras el proceso está en curso (ver FR-009).
5. El paciente confirma la reserva de la franja seleccionada.
6. El sistema valida que el bloqueo temporal de la franja siga vigente.
7. El sistema registra la cita, asociando la cuenta del paciente autenticado con el médico y la franja
   horaria seleccionados, y libera el bloqueo temporal dejando la franja como ocupada de forma definitiva.
8. El sistema envía un mensaje de confirmación al número de WhatsApp registrado en el perfil del paciente.
9. El sistema muestra al paciente la confirmación de que su cita quedó registrada.

**Acceptance Scenarios**:

1. **Given** el paciente accede al sistema fuera del horario de atención telefónica e inició sesión con su
   cuenta, **When** elige médico, fecha y hora disponibles y confirma, **Then** la cita queda registrada
   asociada a su cuenta y el paciente recibe confirmación por WhatsApp en el número registrado en su perfil.
2. **Given** el paciente intenta seleccionar una franja ya ocupada, **When** intenta confirmarla, **Then**
   el sistema la muestra como no disponible y lo invita a elegir otra franja.
3. **Given** el paciente seleccionó una franja disponible y el sistema la bloqueó temporalmente para él,
   **When** otro paciente intenta seleccionar esa misma franja mientras el bloqueo está vigente, **Then**
   el sistema la muestra como no disponible para el segundo paciente.
4. **Given** una persona no ha iniciado sesión, **When** intenta acceder al flujo de selección o confirmación
   de una cita, **Then** el sistema se lo impide y lo dirige a iniciar sesión primero.
5. **Given** el bloqueo temporal de la franja seleccionada por el paciente expiró antes de confirmar (han
   pasado más de 5 minutos desde la selección sin confirmar), **When** el paciente intenta confirmar,
   **Then** el sistema rechaza la confirmación, informa que la franja ya no está disponible y lo invita a
   elegir otra.
6. **Given** el paciente ya tiene 2 citas activas registradas, **When** intenta confirmar una nueva reserva,
   **Then** el sistema rechaza la confirmación e informa que alcanzó el límite de citas activas permitidas.

---

### Edge Cases

- **Franja liberada por expiración del bloqueo**: si el paciente no completa la confirmación dentro de los
  5 minutos posteriores a seleccionar la franja (ver Escenario 5 y FR-010), la franja MUST volver a
  mostrarse como disponible para otros pacientes.
- **Doble confirmación accidental**: si el paciente confirma más de una vez sobre la misma solicitud (por
  ejemplo, por doble clic o reintento tras perder la conexión), el sistema NUNCA MUST registrar más de una
  cita para esa misma solicitud de reserva.
- **Franjas no ofrecibles**: el sistema NUNCA MUST mostrar como disponibles franjas horarias pasadas o que
  estén fuera de la agenda del médico seleccionado (ver FR-002).
- **Acceso sin sesión iniciada**: si una persona sin sesión iniciada intenta seleccionar o confirmar una
  franja, el sistema MUST bloquear la acción y dirigirla a iniciar sesión (ver Escenario 4 y FR-005).
- **Falla en el envío de la confirmación por WhatsApp**: si el mensaje no puede entregarse, el sistema MUST
  reintentar el envío automáticamente (ver FR-016) sin revertir ni bloquear la cita ya registrada. Si tras
  los reintentos el envío sigue fallando, el fallo MUST quedar registrado para seguimiento (ver FR-017).

## Requirements *(mandatory)*

### Functional Requirements

**Consulta y selección**

- **FR-001**: El sistema MUST permitir a cualquier paciente autenticado consultar los médicos y sus franjas
  horarias disponibles (fecha y hora) en cualquier momento, sin restricción de horario de atención
  telefónica.
- **FR-002**: El sistema MUST mostrar como disponibles únicamente franjas horarias futuras que estén dentro
  de la agenda del médico seleccionado; franjas pasadas o fuera de agenda NUNCA MUST ofrecerse como
  seleccionables.
- **FR-003**: El sistema MUST permitir al paciente seleccionar un médico y una franja horaria disponible.
- **FR-004**: El sistema MUST mostrar como "no disponible" cualquier franja horaria que ya esté ocupada o
  temporalmente bloqueada por otro paciente, impidiendo su selección.

**Identificación del paciente**

- **FR-005**: El sistema MUST exigir que el paciente tenga una sesión iniciada (cuenta previamente creada y
  autenticada) antes de permitirle seleccionar o confirmar una franja horaria. La creación de la cuenta y el
  mecanismo de autenticación en sí no forman parte de esta historia (ver Assumptions).
- **FR-006**: El sistema MUST usar el número de WhatsApp ya registrado en el perfil de la cuenta del
  paciente autenticado para enviar la confirmación de la cita, sin volver a solicitarlo durante el flujo de
  reserva.

**Bloqueo temporal de la franja**

- **FR-009**: Al seleccionar una franja disponible, el sistema MUST bloquearla temporalmente en favor del
  paciente que la seleccionó, impidiendo que otro paciente la seleccione o confirme mientras el bloqueo
  esté vigente.
- **FR-010**: Si el paciente no confirma la reserva dentro de los **5 minutos** posteriores a seleccionar la
  franja, el sistema MUST liberar el bloqueo automáticamente y volver a mostrar la franja como disponible.

**Confirmación y registro de la cita**

*(Nota de numeración: FR-007 se retiró durante la ronda de clarificación del 2026-07-02 al pasar de
"datos ingresados ad-hoc" a "cuenta con login previo" — ver sección Clarifications. El salto de
FR-006 a FR-008 es intencional, no un requisito faltante.)*

- **FR-008**: El sistema MUST permitir al paciente autenticado confirmar la reserva únicamente cuando la
  franja seleccionada siga bloqueada a su favor.
- **FR-011**: Al confirmarse, el sistema MUST registrar la cita de forma única, asociando la cuenta del
  paciente autenticado con el médico y la franja horaria seleccionados.
- **FR-012**: El sistema NUNCA MUST registrar más de una cita a partir de una misma solicitud de
  confirmación (protección ante doble envío o reintento).
- **FR-013**: Si al momento de confirmar la franja ya no está disponible (porque el bloqueo expiró o la
  franja fue ocupada por otra vía), el sistema MUST rechazar la confirmación, informar que la franja ya no
  está disponible e invitar al paciente a elegir otra.
- **FR-018**: El sistema MUST impedir que un paciente confirme una nueva cita si ya tiene **2 citas
  activas** registradas (citas confirmadas cuya franja horaria aún no ha pasado), informándole que alcanzó
  el límite de citas activas permitidas.

**Notificación**

- **FR-014**: Una vez registrada la cita, el sistema MUST enviar un mensaje de confirmación por WhatsApp al
  número registrado en el perfil del paciente autenticado.
- **FR-016**: Si el envío del mensaje de confirmación falla, el sistema MUST reintentarlo automáticamente un
  número de veces antes de marcarlo como fallido. *(El número exacto de reintentos y el intervalo entre
  ellos no están definidos por esta especificación; ver Assumptions.)*
- **FR-017**: El registro de la cita (FR-011) NUNCA MUST revertirse ni bloquearse por una falla en el envío
  de la confirmación por WhatsApp; si tras los reintentos el envío sigue fallando, el sistema MUST dejar
  ese fallo visible para seguimiento.

**Restricción de canal**

- **FR-015**: El flujo completo de inicio de sesión, consulta, selección y confirmación MUST poder
  completarse sin requerir una llamada telefónica ni intervención de una persona del centro médico.

### Key Entities

- **Paciente (cuenta)**: cuenta previamente creada del paciente, fuera de alcance de esta historia; se
  asume que incluye al menos nombre completo, documento de identificación y número de WhatsApp ya
  registrados y accesibles tras iniciar sesión.
- **Médico**: profesional de salud que atiende citas; tiene una agenda de franjas horarias que pueden estar
  disponibles, bloqueadas temporalmente u ocupadas.
- **Franja Horaria**: unidad de tiempo (fecha y hora) asociada a un médico; tiene estado — disponible,
  bloqueada temporalmente a favor de un paciente, u ocupada — y, cuando está ocupada, está asociada a una
  única cita.
- **Cita**: registro que resulta de una confirmación exitosa; vincula la cuenta del paciente autenticado con
  el médico y la franja horaria confirmados.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los pacientes con cuenta ya creada pueden completar el flujo de reserva (iniciar
  sesión, consultar, seleccionar y confirmar) sin necesidad de llamar ni de que intervenga personal del
  centro médico, en cualquier momento del día.
- **SC-002**: El sistema permite consultar y reservar citas de forma continua los 7 días de la semana, sin
  restringir el acceso a un horario de atención telefónica.
- **SC-003**: El 0% de las citas registradas queda asociado a una franja horaria que ya estuviera ocupada
  o bloqueada a favor de otro paciente al momento de confirmarse (ninguna doble reserva).
- **SC-004**: El 100% de los intentos de seleccionar o confirmar una franja no disponible reciben una
  indicación clara de que no está disponible y la posibilidad de elegir otra, sin necesidad de contactar al
  centro médico.
- **SC-005**: El 100% de las citas registradas exitosamente generan un intento de envío de confirmación al
  número de WhatsApp registrado en el perfil del paciente, incluyendo reintentos automáticos si el primer
  envío falla.
- **SC-006**: El 100% de los intentos de seleccionar o confirmar una franja sin una sesión de paciente
  iniciada son rechazados y la persona es dirigida a iniciar sesión.
- **SC-007**: El 100% de las citas registradas permanecen válidas y consultables por el paciente incluso
  cuando el envío de la confirmación por WhatsApp falla de forma definitiva tras los reintentos.
- **SC-008**: El 100% de los intentos de confirmación de un paciente que ya tiene 2 citas activas son
  rechazados con una indicación clara de que alcanzó el límite permitido.

## Assumptions

- El paciente MUST tener una cuenta previamente creada y autenticarse (login) antes de reservar; el
  registro/alta de la cuenta, la gestión de credenciales y el mecanismo de autenticación en sí son una
  historia distinta, fuera de alcance de US-01 (decisión confirmada el 2026-07-02).
- Se asume que la cuenta del paciente, gestionada fuera de esta historia, ya contiene nombre completo,
  documento de identificación y un número de WhatsApp válido, accesibles una vez iniciada la sesión.
- El número de WhatsApp usado para la confirmación es el registrado en el perfil de la cuenta del paciente
  autenticado, no uno ingresado ad-hoc durante la reserva (decisión confirmada el 2026-07-02).
- El sistema bloquea temporalmente la franja seleccionada mientras el paciente completa el flujo, por
  **5 minutos**, para evitar que dos pacientes confirmen la misma franja libre al mismo tiempo (decisión
  confirmada el 2026-07-02).
- Si el envío de la confirmación por WhatsApp falla, el sistema reintenta automáticamente antes de marcarlo
  como fallido, sin afectar el registro ya hecho de la cita (decisión confirmada el 2026-07-02, ver FR-016 y
  FR-017).
- El número de reintentos automáticos de envío y el intervalo entre ellos **MUST tratarse como un
  parámetro configurable** definido en el diseño técnico (`research.md` §7), no como un valor de
  negocio fijo impuesto por esta especificación — esta historia solo exige que exista reintento
  automático (FR-016), no un conteo específico.
- Un mismo paciente no puede tener más de **2 citas activas** (confirmadas, con franja horaria aún futura)
  al mismo tiempo; una nueva confirmación que exceda ese límite MUST rechazarse (decisión confirmada el
  2026-07-02, ver FR-018).
- La disponibilidad de médicos, fechas y horas ya existe como información consultable por el sistema; esta
  historia no cubre la creación ni administración de agendas médicas, solo su consulta, bloqueo temporal y
  reserva.
- No se especifican políticas de cancelación, reprogramación o límite de citas por paciente; esas
  capacidades no están cubiertas por esta historia.
