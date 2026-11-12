package shortUrl.Entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Url {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "url_original")
    private String urlOriginal;
    @Column(name = "short_url")
    private String shortUrl;
    @Column(name = "LocalDateTime")
    private LocalDateTime localDateTime;

    @Column(name = "click_count", nullable = false, insertable = false, updatable = false)
    private long clickCount;

    @Column(name = "list_id")
    private Long listId;

    @Column(name = "management_key_hash")
    private String managementKeyHash;

}
