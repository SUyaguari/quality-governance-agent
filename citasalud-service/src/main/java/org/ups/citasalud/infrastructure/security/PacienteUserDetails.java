package org.ups.citasalud.infrastructure.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * {@link UserDetails} que además transporta el perfil del paciente (id, nombre, documento,
 * WhatsApp) necesario para FR-006/FR-011/FR-014. El registro/alta de esta cuenta está fuera de
 * alcance de esta historia (ver research.md §4); esta clase solo modela el perfil ya existente.
 */
public class PacienteUserDetails implements UserDetails {

    private final UUID pacienteId;
    private final String nombreCompleto;
    private final String documentoIdentificacion;
    private final String numeroWhatsApp;
    private final String username;
    private final String password;

    public PacienteUserDetails(UUID pacienteId, String nombreCompleto, String documentoIdentificacion,
                                String numeroWhatsApp, String username, String password) {
        this.pacienteId = pacienteId;
        this.nombreCompleto = nombreCompleto;
        this.documentoIdentificacion = documentoIdentificacion;
        this.numeroWhatsApp = numeroWhatsApp;
        this.username = username;
        this.password = password;
    }

    public UUID getPacienteId() {
        return pacienteId;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getDocumentoIdentificacion() {
        return documentoIdentificacion;
    }

    public String getNumeroWhatsApp() {
        return numeroWhatsApp;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_PACIENTE"));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
