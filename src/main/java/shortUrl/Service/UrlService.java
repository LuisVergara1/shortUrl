package shortUrl.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;
import shortUrl.DTO.AliasStats;
import shortUrl.DTO.UrlStats;
import shortUrl.Entity.Url;
import shortUrl.Repository.UrlRepository;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Optional;

@Service
public class UrlService {
    private final ManagementAttemptLimiter managementAttempts = new ManagementAttemptLimiter(Clock.systemUTC());

    @Autowired
    private UrlRepository urlRepository;

    @Value("${domain}")
    private String domain = "http://localhost:8080/r/"; 

    @Value("${BLOCKED_DOMAINS}")
    private String blockedDomainsEnv;

    @Value("${PROTECTED_URLS}")
    private String protectedUrlsEnv;

    @Value("${LINK_TTL_MINUTES:15}")
    private int linkTtlMinutes;

    private Set<String> blockedDomains = new HashSet<>();
    private Set<String> protectedUrls = new HashSet<>();

    

    @PostConstruct
    private void init() {
        if (linkTtlMinutes < 1) {
            throw new IllegalArgumentException("LINK_TTL_MINUTES debe ser un entero positivo");
        }
        if (blockedDomainsEnv != null && !blockedDomainsEnv.isEmpty()) {
            String[] domains = blockedDomainsEnv.split(",");
            for (String domain : domains) {
                if (!domain.isBlank()) {
                    blockedDomains.add(normalizeHost(domain.strip()));
                }
            }
        }
          // Inicializar URLs protegidas
        if (protectedUrlsEnv != null && !protectedUrlsEnv.isEmpty()) {
            String[] urls = protectedUrlsEnv.split(",");
            for (String url : urls) {
                protectedUrls.add(url.trim());
            }
        }
    }
    

    private boolean isBlockedHost(String host) {
        String normalized = normalizeHost(host);
        return blockedDomains.stream().anyMatch(
                blocked -> normalized.equals(blocked) || normalized.endsWith("." + blocked));
    }

    private static String normalizeHost(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

  public boolean isProtectedUrl(String shortUrl) {
    return protectedUrls.contains(shortUrl);
}

    public record CreatedLink(String url, String managementKey) {
    }

    @Transactional
    public CreatedLink createLink(String original, String customCode, String alias, String key, String clientIp) {
        if (isBlockedHost(requireHttpUrl(original).getHost())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este sitio web está bloqueado");
        }
        if (customCode != null && !customCode.matches("[a-zA-Z0-9]{1,255}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código solo admite letras y números");
        }

        Url link = new Url();
        String anonymousKey = null;
        if (alias == null || alias.isBlank()) {
            anonymousKey = ManagementKeys.generate();
            link.setManagementKeyHash(ManagementKeys.hash(anonymousKey));
        } else {
            String normalized = normalizeAlias(alias);
            if (normalized.length() > 64 || normalized.indexOf(0) >= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alias inválido");
            }
            if (key == null || key.isBlank() || key.length() < 8 || key.length() > 128) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clave debe tener entre 8 y 128 caracteres");
            }
            link.setListId(resolveList(normalized, key, clientIp));
        }

        String code = customCode == null ? UUID.randomUUID().toString().replace("-", "").substring(0, 16) : customCode;
        link.setShortUrl(code);
        link.setUrlOriginal(original);
        link.setLocalDateTime(LocalDateTime.now().plusMinutes(linkTtlMinutes));
        try {
            urlRepository.saveAndFlush(link);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El código corto ya está en uso", exception);
        }
        return new CreatedLink(domain + code, anonymousKey);
    }

    private long resolveList(String alias, String key, String clientIp) {
        var listId = urlRepository.findListIdByAlias(alias);
        if (listId.isEmpty()) {
            int reserved = urlRepository.reserveAlias(alias, ManagementKeys.hash(key));
            listId = urlRepository.findListIdByAlias(alias);
            if (reserved == 1) {
                return listId.orElseThrow();
            }
        }
        long id = listId.orElseThrow();
        verifyListKey(id, key, clientIp);
        return id;
    }

    private static String normalizeAlias(String alias) {
        return alias.strip().toLowerCase(Locale.ROOT);
    }

    private URI requireHttpUrl(String url) {
        try {
            if (url != null) {
                URI uri = new URI(url);
                if (uri.getHost() != null
                        && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                    return uri;
                }
            }
        } catch (URISyntaxException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa una URL HTTP o HTTPS válida", exception);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa una URL HTTP o HTTPS válida");
    }

    @Transactional
    public Optional<String> registerClick(String shortUrl) {
      return urlRepository.registerClick(shortUrl, LocalDateTime.now());
    }

    public Optional<UrlStats> getStats(String shortUrl) {
      return Optional.ofNullable(urlRepository.findByShortUrl(shortUrl))
          .map(url -> toStats(url, LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public Optional<AliasStats> getAliasStats(String alias, Pageable pageable) {
      String normalizedAlias = normalizeAlias(alias);
      LocalDateTime now = LocalDateTime.now();
      return urlRepository.findListIdByAlias(normalizedAlias).map(listId -> {
        var links = urlRepository.findLinksByListId(listId, pageable).map(url -> toStats(url, now));
        return new AliasStats(normalizedAlias, links.getContent(), links.getNumber(), links.getSize(),
            links.getTotalElements(), links.getTotalPages());
      });
    }

    private UrlStats toStats(Url url, LocalDateTime now) {
      return new UrlStats(url.getShortUrl(), domain + url.getShortUrl(), url.getClickCount(), url.getLocalDateTime(),
          isActive(url, now));
    }

    private boolean isActive(Url url, LocalDateTime now) {
        return url.getLocalDateTime() == null || url.getLocalDateTime().isAfter(now);
    }

    public int getLinkTtlMinutes() {
        return linkTtlMinutes;
    }

    public String getShortUrlBase() {
        return domain;
    }

    //Redireccionar la URL
    public Url getOriginalUrl(String shortUrl) {
      // Verifica que shortUrl no sea nulo
      if (shortUrl == null) {
          return null;
      }
  
      // Encuentra la URL original en la base de datos usando el código extraído
      Url url = urlRepository.findByShortUrl(shortUrl);
  
      // Verifica si la URL existe y si no ha expirado
      if (url != null && isActive(url, LocalDateTime.now())) {
          return url;
      }
  
      return null;
  }


    @Transactional
    public void extendTime(String shortUrl, String key, String clientIp) {
        Url url = requireManageableLink(shortUrl, key, clientIp);
        url.setLocalDateTime(LocalDateTime.now().plusMinutes(linkTtlMinutes));
    }

    @Transactional
    public void deleteUrl(String shortUrl, String key, String clientIp) {
        urlRepository.delete(requireManageableLink(shortUrl, key, clientIp));
    }

    private Url requireManageableLink(String shortUrl, String key, String clientIp) {
        Url url = urlRepository.findByShortUrl(shortUrl);
        if (url == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos el enlace");
        }
        if (url.getLocalDateTime() == null || isProtectedUrl(shortUrl)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este enlace está protegido");
        }
        if (url.getListId() == null) {
            managementAttempts.verify(clientIp, "link:" + url.getId(),
                    () -> ManagementKeys.matches(key, url.getManagementKeyHash()));
        } else {
            verifyListKey(url.getListId(), key, clientIp);
        }
        return url;
    }

    private void verifyListKey(long listId, String key, String clientIp) {
        managementAttempts.verify(clientIp, "list:" + listId,
                () -> ManagementKeys.matches(key, urlRepository.findListKeyHash(listId).orElse(null)));
    }
}
