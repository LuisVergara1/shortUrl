package shortUrl.Controller;

import java.util.Map;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import shortUrl.DTO.AliasStats;
import shortUrl.DTO.FullUrl;
import shortUrl.DTO.RequestUrl;
import shortUrl.DTO.UrlStats;
import shortUrl.Service.UrlService;
import shortUrl.configuration.ClientIp;

@RestController
@RequestMapping("/api/url")
@Tag(name = "Url Controller", description = "EndPoint Disponibles ")
public class UrlController {

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "linkTtlMinutes", urlService.getLinkTtlMinutes(), "shortUrlBase", urlService.getShortUrlBase()));
    }

    @GetMapping("/{shortUrl}/stats")
    public ResponseEntity<UrlStats> getStats(@PathVariable String shortUrl) {
        return urlService.getStats(shortUrl)
                .map(stats -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(stats))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/stats/by-alias")
    public ResponseEntity<AliasStats> getAliasStats(@RequestParam String alias,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        if (alias.isBlank() || alias.strip().length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El alias debe tener entre 1 y 64 caracteres");
        }
        if (page < 0 || size < 1 || size > 50 || (long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginación inválida");
        }
        return urlService.getAliasStats(alias, PageRequest.of(page, size))
                .map(stats -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(stats))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @Autowired
    private UrlService urlService;

    @Autowired
    private ClientIp clientIp;

    @PostMapping("/create")
    public ResponseEntity<String> createUrl(@RequestBody RequestUrl request, HttpServletRequest httpRequest) {
        return created(urlService.createLink(request.getUrlOriginal(), null, request.getAlias(),
                request.getManagementKey(), clientIp.resolve(httpRequest)));
    }

    @PostMapping("/createfull")
    public ResponseEntity<String> createCustomUrl(@RequestBody FullUrl request, HttpServletRequest httpRequest) {
        if (request.getShortUrl() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa un código personalizado");
        }
        return created(urlService.createLink(request.getUrlOriginal(), request.getShortUrl(),
                request.getAlias(), request.getManagementKey(), clientIp.resolve(httpRequest)));
    }

    private ResponseEntity<String> created(UrlService.CreatedLink result) {
        var response = ResponseEntity.ok().cacheControl(CacheControl.noStore());
        if (result.managementKey() != null) {
            response.header("X-Management-Key", result.managementKey());
        }
        return response.body(result.url());
    }

    @PutMapping("/extend")
    public ResponseEntity<String> extendTime(@RequestBody RequestUrl request, HttpServletRequest httpRequest) {
        urlService.extendTime(request.getUrlOriginal(), request.getManagementKey(), clientIp.resolve(httpRequest));
        return ResponseEntity.ok("URL renovada");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUrl(@RequestBody RequestUrl request, HttpServletRequest httpRequest) {
        urlService.deleteUrl(request.getUrlOriginal(), request.getManagementKey(), clientIp.resolve(httpRequest));
        return ResponseEntity.ok("URL eliminada");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleRequestError(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).headers(exception.getHeaders()).cacheControl(CacheControl.noStore())
                .body(exception.getReason());
    }
}
