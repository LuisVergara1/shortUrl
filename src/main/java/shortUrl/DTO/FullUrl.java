package shortUrl.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FullUrl {

    private String urlOriginal;
    private String shortUrl;
}
