package shortUrl.Controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
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
      @ApiResponse(responseCode = "200", description = "Redirects to the original URL.", content = { @Content(schema = @Schema(implementation = String.class), mediaType = "application/json") }),
      @ApiResponse(responseCode = "404", description = "Short URL not found or expired.", content = { @Content(schema = @Schema(implementation = String.class), mediaType = "application/json") })
  })
    @GetMapping("/{shortUrl}")
    public void redirectUrl(@PathVariable String shortUrl , HttpServletResponse response) throws IOException
    {
        Url url = urlService.getOriginalUrl(shortUrl);

        if (url != null) {
            response.sendRedirect(url.getUrlOriginal());
        }else
        {
            response.sendRedirect(domainError);;
        }
    }
 
}
