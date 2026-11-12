package shortUrl.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import shortUrl.Entity.Url;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Url findByShortUrl(String shortUrl);

    Boolean existsByShortUrl(String shortUrl);

    @Query(value = "SELECT id FROM {h-schema}link_list WHERE alias = :alias", nativeQuery = true)
    Optional<Long> findListIdByAlias(@Param("alias") String alias);

    @Modifying
    @Query(value = """
            INSERT INTO {h-schema}link_list (alias, management_key_hash) VALUES (:alias, :hash)
            ON CONFLICT (alias) DO NOTHING
            """, nativeQuery = true)
    int reserveAlias(@Param("alias") String alias, @Param("hash") String hash);

    @Query(value = "SELECT management_key_hash FROM {h-schema}link_list WHERE id = :id", nativeQuery = true)
    Optional<String> findListKeyHash(@Param("id") long id);

    @Query(value = "SELECT u.* FROM {h-schema}url u WHERE u.list_id = :listId ORDER BY u.id DESC",
            countQuery = "SELECT count(*) FROM {h-schema}url WHERE list_id = :listId", nativeQuery = true)
    Page<Url> findLinksByListId(@Param("listId") long listId, Pageable pageable);

    @Query(value = """
            WITH clicked AS (
                UPDATE {h-schema}url
                SET click_count = click_count + 1
                WHERE short_url = :shortUrl AND (local_date_time IS NULL OR local_date_time > :now)
                RETURNING url_original
            )
            SELECT url_original FROM clicked
            """, nativeQuery = true)
    Optional<String> registerClick(@Param("shortUrl") String shortUrl, @Param("now") LocalDateTime now);
} 
