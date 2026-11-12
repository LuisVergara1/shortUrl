package shortUrl.Controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import shortUrl.Entity.Url;
import shortUrl.Service.UrlService;

@RestController
@RequestMapping("/r")
public class RedirectController {


    @Autowired
    UrlService urlService ;
    //-----------------------------------------------

    @Value("${DOMAIN_ERROR}")
    private String domainError;


    //EndPoint Redirect URL
    @Operation(
      summary = "Redirect to original URL",
      description = "Redirects to the original URL based on the provided short URL."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "302", description = "Redirects to the original URL or the error page.")
  })
    @GetMapping("/{shortUrl}")
    public void redirectUrl(@PathVariable String shortUrl, HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-store");
        String destination;
        if ("HEAD".equals(request.getMethod())) {
            Url url = urlService.getOriginalUrl(shortUrl);
            destination = url == null ? domainError : url.getUrlOriginal();
        } else {
            destination = urlService.registerClick(shortUrl).orElse(domainError);
        }
        response.sendRedirect(destination);
    }
 
}
