package shortUrl.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class ManagementAttemptLimiter {
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_BUCKETS = 10_000;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final Clock clock;
    private final Map<Target, Window> attempts = new HashMap<>();
    private Instant nextCleanup = Instant.MIN;

    ManagementAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    void verify(String clientIp, String target, BooleanSupplier matches) {
        var bucket = new Target(clientIp, target);
        Window window = reserve(bucket);
        boolean failed = false;
        try {
            failed = !matches.getAsBoolean();
            if (failed) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ingresa la clave de gestión correcta");
            }
        } finally {
            if (!failed) {
                release(bucket, window);
            }
        }
    }

    private synchronized Window reserve(Target bucket) {
        Instant now = clock.instant();
        if (!now.isBefore(nextCleanup)) {
            attempts.values().removeIf(window -> !now.isBefore(window.expiresAt));
            nextCleanup = now.plusSeconds(30);
        }
        Window window = attempts.get(bucket);
        if (window == null || !now.isBefore(window.expiresAt)) {
            if (window == null && attempts.size() >= MAX_BUCKETS) {
                throw new TooManyAttempts(30);
            }
            window = new Window(now.plus(WINDOW));
            attempts.put(bucket, window);
        }
        if (window.used >= MAX_ATTEMPTS) {
            long remainingMillis = Duration.between(now, window.expiresAt).toMillis();
            throw new TooManyAttempts(Math.max(1, (remainingMillis + 999) / 1000));
        }
        window.used++;
        return window;
    }

    private synchronized void release(Target bucket, Window window) {
        if (--window.used == 0) {
            attempts.remove(bucket, window);
        }
    }

    private record Target(String clientIp, String resource) {
    }

    private static final class Window {
        private final Instant expiresAt;
        private int used;

        private Window(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }
    }

    private static final class TooManyAttempts extends ResponseStatusException {
        private final long retryAfter;

        private TooManyAttempts(long retryAfter) {
            super(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Espera un momento y vuelve a probar.");
            this.retryAfter = retryAfter;
        }

        @Override
        public HttpHeaders getHeaders() {
            var headers = new HttpHeaders();
            headers.set(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
            return headers;
        }
    }
}
