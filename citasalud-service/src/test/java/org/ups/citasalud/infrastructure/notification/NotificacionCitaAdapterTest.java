package org.ups.citasalud.infrastructure.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.ups.citasalud.domain.model.Cita;
import org.ups.citasalud.domain.port.NotificacionCitaPort;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * BDD (Given-When-Then), integración con un stub HTTP del proveedor de WhatsApp
 * ({@link MockRestServiceServer}, de spring-test). {@link WhatsAppHttpSender} se envuelve en un
 * proxy AOP con el mismo interceptor de reintentos que instala {@code @EnableRetry} en tiempo de
 * ejecución, para probar el comportamiento real de FR-016 sin levantar todo el contexto Spring.
 * FR-014/FR-016/FR-017; SC-007.
 */
class NotificacionCitaAdapterTest {

    private static final String URL = "http://test-whatsapp-provider.invalid/v1/messages";

    private MockRestServiceServer server;
    private NotificacionCitaPort adapter;

    @BeforeEach
    void configurarAdapterConProxyDeReintentos() {
        RestClient.Builder builder = RestClient.builder();
        this.server = MockRestServiceServer.bindTo(builder).build();

        WhatsAppProperties properties = new WhatsAppProperties();
        properties.setBaseUrl(URL);
        properties.setReintentos(3);
        properties.setBackoffInicialMs(1);

        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(properties.getReintentos())
                .fixedBackoff(properties.getBackoffInicialMs())
                .retryOn(RestClientException.class)
                .build();
        MethodInterceptor retryInterceptor = RetryInterceptorBuilder.stateless()
                .retryOperations(retryTemplate)
                .build();

        WhatsAppHttpSender senderTarget = new WhatsAppHttpSender(builder, properties);
        ProxyFactory proxyFactory = new ProxyFactory(senderTarget);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(retryInterceptor);
        WhatsAppHttpSender senderConReintentos = (WhatsAppHttpSender) proxyFactory.getProxy();

        this.adapter = new NotificacionCitaAdapter(senderConReintentos);
    }

    private Cita citaDePrueba() {
        return Cita.nueva(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-08-01T10:00:00Z"));
    }

    @Test
    @DisplayName("Given el proveedor responde exitosamente, When se envía la confirmación, Then no lanza excepción (FR-014)")
    void given_proveedorResponde_when_enviarConfirmacion_then_noLanzaExcepcion() {
        server.expect(requestTo(URL)).andRespond(withSuccess());

        adapter.enviarConfirmacion(citaDePrueba(), "María Pérez", "+593987654321");

        server.verify();
    }

    @Test
    @DisplayName("Given el proveedor falla dos veces y luego responde, When se envía, Then reintenta automáticamente y termina en éxito (FR-016)")
    void given_proveedorFallaDosVeces_when_enviarConfirmacion_then_reintentaYTieneExito() {
        server.expect(requestTo(URL)).andRespond(withServerError());
        server.expect(requestTo(URL)).andRespond(withServerError());
        server.expect(requestTo(URL)).andRespond(withSuccess());

        adapter.enviarConfirmacion(citaDePrueba(), "María Pérez", "+593987654321");

        server.verify();
    }

    @Test
    @DisplayName("Given el proveedor falla en todos los reintentos, When se envía, Then lanza NotificacionFallidaException tras agotarlos (FR-017/SC-007)")
    void given_proveedorFallaSiempre_when_enviarConfirmacion_then_lanzaNotificacionFallidaTrasAgotarReintentos() {
        server.expect(requestTo(URL)).andRespond(withServerError());
        server.expect(requestTo(URL)).andRespond(withServerError());
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> adapter.enviarConfirmacion(citaDePrueba(), "María Pérez", "+593987654321"))
                .isInstanceOf(NotificacionCitaPort.NotificacionFallidaException.class);

        server.verify();
    }
}
