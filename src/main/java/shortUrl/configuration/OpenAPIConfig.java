package shortUrl.configuration;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenAPIConfig {
    
    @Bean
    public OpenAPI myOpenAPI(){
        Server devServer = new Server();
        devServer.setUrl("http://localhost:8080");
        devServer.description("Server URL in Development environment");

        Contact contact = new Contact();
        contact.setEmail("luismarcelo.v.c@hotmail.com");
        contact.setName("Luis Vergara");
        contact.setUrl("https://www.luisvergara.dev");

        License mitLicense = new License().name("MIT Licence").url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
        .title("Api ShortUrl")
        .version("1.0")
        .contact(contact)
        .description("EndPoint ShortUrl").termsOfService(null)
        .license(mitLicense);
        
        return new OpenAPI().info(info).servers(List.of(devServer));
    }


}
