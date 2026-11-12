package shortUrl.Service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ManagementAttemptLimiterTests {
    @Test
    void blocksOnlyTheExhaustedIpAndTargetUntilTheWindowExpires() {
        var clock = new TestClock();
        var limiter = new ManagementAttemptLimiter(clock);
        String ip = "192.0.2.1";
        String target = "list:42";
        for (int i = 0; i < 6; i++) {
            limiter.verify(ip, target, () -> true);
        }
        for (int i = 0; i < 5; i++) {
            var error = assertThrows(ResponseStatusException.class, () -> limiter.verify(ip, target, () -> false));
            assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        }
        var error = assertThrows(ResponseStatusException.class, () -> limiter.verify(ip, target, () -> {
            throw new AssertionError("No se debe verificar la clave mientras el límite esté activo");
        }));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, error.getStatusCode());
        assertEquals("600", error.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertDoesNotThrow(() -> limiter.verify("192.0.2.2", target, () -> true));
        assertDoesNotThrow(() -> limiter.verify(ip, "link:42", () -> true));

        clock.now = clock.now.plusSeconds(600);
        assertDoesNotThrow(() -> limiter.verify(ip, target, () -> true));
    }

    private static final class TestClock extends Clock {
        private Instant now = Instant.EPOCH;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
