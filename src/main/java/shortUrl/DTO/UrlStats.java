package shortUrl.DTO;

import java.time.LocalDateTime;

public record UrlStats(String shortCode, String shortUrl, long clickCount, LocalDateTime expiresAt, boolean active) {
}
