package shortUrl.Controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import shortUrl.DTO.FullUrl;
import shortUrl.DTO.RequestUrl;
import shortUrl.Service.UrlService;

@RestController
@RequestMapping("/api/url")
@Tag(name = "Url Controller", description = "EndPoint Disponibles ")
public class UrlController {

    
    @Autowired
    private UrlService urlService;
    //-------------------------------------------
    //EndPoint Create Url
    @Operation(
      summary = "Create a short URL",
      description = "Generates a short URL for the given original URL. The response contains the short URL."
      // , tags = {  "urls", "shortener", "creation"  }
      )
    @ApiResponses({
      @ApiResponse(responseCode = "200",description = "Generates a short URL for the given original URL. The response contains the short URL.", content = { @Content(schema = @Schema(implementation = String.class), mediaType = "application/json") }),
      @ApiResponse(responseCode = "404", description ="Invalid input or URL creation failed", content = { @Content(schema = @Schema(implementation = String.class), mediaType = "application/json") })})
    @PostMapping("/create")
    public ResponseEntity<String> createUrl(@RequestBody RequestUrl requestUrl)
    {
        String url = urlService.createUrl(requestUrl);
        if (url != null) {
            return ResponseEntity.ok(url);
        }
        else{
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se pudo crear la URL");
        }
        
    }

    //--------------------------------------

    //EndPoint Create Full Url
    @Operation(
    summary = "Create a short URL",
    description = "Generates a short URL for the given original URL. The response contains the short URL. Allows specifying a custom short URL."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "The short URL was generated successfully.", content = { @Content(schema = @Schema(implementation = String.class), mediaType = "application/json") }),
    @ApiResponse(responseCode = "400", description = "Invalid input or URL creation failed.", content = { @Content(schema = @Schema(implementation = String.class), mediaType = "application/json") })
})
@PostMapping("/createfull")
public ResponseEntity<String> createUrl(@RequestBody FullUrl fullUrl) {
    // Verificar si la URL está bloqueada
    if (urlService.isBlockedUrl(fullUrl.getUrlOriginal())) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Este sitio web está bloqueado");
    }

    // Verificar si la URL es válida
    if (!urlService.isValidUrl(fullUrl.getUrlOriginal())) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("La URL no es válida");
    }

    if (!fullUrl.getShortUrl().matches("^[a-zA-Z0-9]+$")) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("La URL solo puede contener letras y números");
    }   Boolean exists = urlService.exists(fullUrl.getShortUrl());
    if (exists) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("La URL Personalizada ya existe");
    }
    // Llamar al servicio para crear la URL corta
    String shortUrl = urlService.fullUrl(fullUrl);
    if (shortUrl != null) {
        // Si se genera correctamente, devolver la URL corta con código 200 OK
        return ResponseEntity.ok(shortUrl);
    } else {
        // Si no se pudo crear la URL, devolver un error con código 400 Bad Request
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se pudo crear la URL");
    }
     
}


//------------------------------------------------
//EndPoint ExtendTime
@Operation(
    summary = "Extend the expiration time of a short URL",
    description = "Extends the expiration time of the given short URL by 15 minutes."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "The short URL expiration time was extended successfully.", content = { @Content(schema = @Schema(implementation = String.class), mediaType = "application/json") }),
    @ApiResponse(responseCode = "404", description = "Invalid URL or the URL does not exist.", content = { @Content(schema = @Schema(implementation = String.class), mediaType = "application/json") })
})
    @PutMapping("/extend")
    public ResponseEntity<String>extendTime(@RequestBody RequestUrl requestUrl)
    {
        Boolean result = urlService.extendTime(requestUrl.getUrlOriginal());

        if (result) {
            return ResponseEntity.ok("Url actualizada") ;
        }
        else{
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se pudo Actualizar la URL");
        }

    }
//---------------------------------------
//EndPoint Eliminar URL
@Operation(
    summary = "Delete a short URL",
    description = "Deletes the given short URL from the system."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "The short URL was deleted successfully.", content = { @Content(schema = @Schema(implementation = String.class), mediaType = "application/json") }),
    @ApiResponse(responseCode = "400", description = "The short URL does not exist.", content = { @Content(schema = @Schema(implementation = String.class), mediaType = "application/json") })
})
    @DeleteMapping("/delete")
    public ResponseEntity<String>deleteUrl(@RequestBody RequestUrl requestUrl)
    {
        Boolean result = urlService.deleteUrl(requestUrl.getUrlOriginal());
        if (result) {
            return ResponseEntity.ok("Url Eliminada");
        }else
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se pudo eliminar la URL");
        }
    }
}
