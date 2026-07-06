package org.ups.citasalud.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * FR-005: exige sesión autenticada en los endpoints de esta historia. Autenticación mínima vía
 * HTTP Basic + sesión HTTP (cookie JSESSIONID, coherente con el `sessionAuth` del contrato) — ver
 * research.md §4 y {@link PacienteTestUserDetailsService}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SesionNoIniciadaEntryPoint entryPoint) throws Exception {
        http
                // CSRF deshabilitado: el contrato (citasalud-api.yaml) no define ningún mecanismo
                // de distribución de token CSRF para esta API JSON; esta historia no incluye un
                // cliente basado en formularios de navegador (ver research.md §4).
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(entryPoint));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
