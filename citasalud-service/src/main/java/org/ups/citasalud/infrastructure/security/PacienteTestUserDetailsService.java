package org.ups.citasalud.infrastructure.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Proveedor de autenticación MÍNIMO (in-memory) suficiente para probar esta historia de punta a
 * punta. NO implementa registro/alta de cuentas: es un fixture de perfiles de paciente ya
 * existentes, tal como lo asume el spec (Assumptions) y documenta research.md §4. Debe
 * reemplazarse por la integración real con el sistema de identidad cuando esa historia exista.
 */
@Service
public class PacienteTestUserDetailsService implements UserDetailsService {

    private final Map<String, PacienteUserDetails> pacientes = Map.of(
            "paciente1", new PacienteUserDetails(
                    UUID.fromString("33333333-3333-3333-3333-333333333333"),
                    "María Pérez",
                    "0102030405",
                    "+593987654321",
                    "paciente1",
                    "{noop}paciente1"),
            "paciente2", new PacienteUserDetails(
                    UUID.fromString("44444444-4444-4444-4444-444444444444"),
                    "Carlos Gómez",
                    "0203040506",
                    "+593987654322",
                    "paciente2",
                    "{noop}paciente2")
    );

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        PacienteUserDetails paciente = pacientes.get(username);
        if (paciente == null) {
            throw new UsernameNotFoundException("Paciente no encontrado: " + username);
        }
        return paciente;
    }
}
