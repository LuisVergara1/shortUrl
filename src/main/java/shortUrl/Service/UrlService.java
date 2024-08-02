package shortUrl.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import shortUrl.DTO.FullUrl;
import shortUrl.DTO.RequestUrl;
import shortUrl.Entity.Url;
import shortUrl.Repository.UrlRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

@Service
public class UrlService {
    
    @Autowired
    private UrlRepository urlRepository;

    @Value("${domain}")
    private String domain = "http://localhost:8080/redirect/"; 

     @Value("${BLOCKED_DOMAINS}")
    private String blockedDomainsEnv;

    private Set<String> blockedDomains = new HashSet<>();

    @PostConstruct
    private void init() {
        if (blockedDomainsEnv != null && !blockedDomainsEnv.isEmpty()) {
            String[] domains = blockedDomainsEnv.split(",");
            for (String domain : domains) {
                blockedDomains.add(domain.trim());
            }
        }
    }


    public boolean isBlockedUrl(String url) {
      System.out.println("Verificando URL: " + url);
      for (String blockedDomain : blockedDomains) {
          System.out.println("Comparando con dominio bloqueado: " + blockedDomain);
          if (url.contains(blockedDomain)) {
              System.out.println("URL bloqueada: " + url);
              return true;
          }
      }
      System.out.println("URL permitida: " + url);
      return false;
  }


    //Crea la URL segun la Original 
    public String createUrl(RequestUrl url)
    {
      if (isBlockedUrl(url.getUrlOriginal())) {
        return null;
    }else{
        Url newUrl = new Url();
        newUrl.setUrlOriginal(url.getUrlOriginal());
        newUrl.setLocalDateTime(LocalDateTime.now().plusMinutes(15));
        newUrl.setShortUrl(generateUrl(url.getUrlOriginal().toString()));
        String urlShort = domain + newUrl.getShortUrl(); 
        urlRepository.save(newUrl);
        return urlShort;
      }
    }

    //Genera la URL Ocupando un UUID y tomando los primeros 6 Digitos
    private String generateUrl(String url) {

      UUID uuid = UUID.randomUUID();
      return uuid.toString().substring(0,6);

  }


  public String fullUrl(FullUrl fullUrl) {
    // Verificar si la URL corta ya existe en la base de datos
    boolean exists = urlRepository.existsByShortUrl(fullUrl.getShortUrl());
    
    if (!exists) {
      if (isBlockedUrl(fullUrl.getUrlOriginal())) {
        return null;
      }else{
        // Crear una nueva URL
        Url newUrl = new Url();
        newUrl.setLocalDateTime(LocalDateTime.now().plusMinutes(15));
        newUrl.setShortUrl(fullUrl.getShortUrl());
        newUrl.setUrlOriginal(fullUrl.getUrlOriginal());
        
        // Guardar la nueva URL en la base de datos
        urlRepository.save(newUrl);
        
        // Construir y devolver la URL corta completa
        return domain + newUrl.getShortUrl();
      }
    } else {
        // Devolver un mensaje si la URL corta ya existe
        return "No se pudo crear la URL porque ya existe.";
    }
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
      if (url != null && url.getLocalDateTime().isAfter(LocalDateTime.now())) {
          return url;
      }
  
      return null;
  }


  public Boolean extendTime (String shortUrl)
  {
    Url url = urlRepository.findByShortUrl(shortUrl);

    if (url != null) {
      url.setLocalDateTime(LocalDateTime.now().plusMinutes(15));
      urlRepository.save(url);
      return true;
    }
    return false;
  }

  public Boolean deleteUrl(String shortUrl)
  {
    Url url = urlRepository.findByShortUrl(shortUrl);
    if (url != null) {
      urlRepository.delete(url);
      return true;
    }
    return false;
  }

}
