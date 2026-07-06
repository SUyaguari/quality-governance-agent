package org.ups.citasalud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/** {@code @EnableRetry}: habilita los reintentos de {@code NotificacionCitaAdapter} (FR-016). */
@SpringBootApplication
@EnableRetry
public class CitasaludServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CitasaludServiceApplication.class, args);
    }

}
