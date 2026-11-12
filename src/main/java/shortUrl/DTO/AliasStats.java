package shortUrl.DTO;

import java.util.List;

public record AliasStats(String alias, List<UrlStats> links, int page, int size, long totalLinks, int totalPages) {
}
