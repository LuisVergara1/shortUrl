package shortUrl.configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientIp {
    private final Set<String> trustedProxies;

    public ClientIp(@Value("${TRUSTED_CLOUDFLARE_PROXY_IPS:}") String addresses) {
        this.trustedProxies = Arrays.stream(addresses.split(","))
                .map(String::strip)
                .filter(address -> !address.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public String resolve(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        if (trustedProxies.contains(peer)) {
            String visitor = request.getHeader("CF-Connecting-IP");
            if (visitor != null && visitor.length() <= 45
                    && (visitor.matches("[0-9.]+") || visitor.matches("[0-9a-fA-F.]*:[0-9a-fA-F:.]*"))) {
                return visitor.toLowerCase(java.util.Locale.ROOT);
            }
        }
        return peer;
    }
}
