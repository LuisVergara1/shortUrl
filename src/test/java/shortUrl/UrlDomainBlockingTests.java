package shortUrl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import shortUrl.Service.UrlService;

class UrlDomainBlockingTests {
    private final UrlService service = new UrlService();

    @BeforeEach
    void configureBlockedDomains() {
        ReflectionTestUtils.setField(service, "blockedDomainsEnv", " Bloqueado.Example. , otro.example ");
        ReflectionTestUtils.setField(service, "linkTtlMinutes", 15);
        ReflectionTestUtils.invokeMethod(service, "init");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://bloqueado.example",
        "https://SUB.BLOQUEADO.EXAMPLE.:8443/ruta",
        "https://permitido.example@bloqueado.example/ruta",
        "https://a.b.otro.example"
    })
    void rejectsBlockedHosts(String url) {
        var error = assertThrows(ResponseStatusException.class,
                () -> service.createLink(url, "código inválido", null, null, "127.0.0.1"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("Este sitio web está bloqueado", error.getReason());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://permitido.example/bloqueado.example?host=bloqueado.example",
        "https://falsobloqueado.example",
        "https://bloqueado.example.permitido.example",
        "https://bloqueado.example@permitido.example"
    })
    void acceptsOtherHosts(String url) {
        // An invalid custom code stops creation after the domain check, without writing to the database.
        var error = assertThrows(ResponseStatusException.class,
                () -> service.createLink(url, "código inválido", null, null, "127.0.0.1"));
        assertEquals("El código solo admite letras y números", error.getReason());
    }
}
