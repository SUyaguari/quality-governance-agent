package org.ups.citasalud.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.ups.citasalud.infrastructure.web.generated.model.ErrorResponse;

import java.io.IOException;

/** FR-005/SC-006: cuerpo de error del contrato (código NO_AUTENTICADO) cuando no hay sesión. */
@Component
public class SesionNoIniciadaEntryPoint implements AuthenticationEntryPoint {

    // ObjectMapper propio (no inyectado): este componente solo serializa un ErrorResponse de 2
    // campos y así no depende del bean ObjectMapper de la app ni de su disponibilidad en
    // contextos de prueba recortados (@WebMvcTest con @Import explícito).
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // Content-Type con charset explícito + escritura por OutputStream: evita que el
        // contenedor use ISO-8859-1 por defecto en el PrintWriter y corrompa acentos (p. ej.
        // "sesión" -> "sesi?n").
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErrorResponse body = new ErrorResponse(ErrorResponse.CodeEnum.NO_AUTENTICADO,
                "El paciente no tiene una sesión iniciada");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
