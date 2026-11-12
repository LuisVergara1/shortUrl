package shortUrl.Service;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class ManagementKeys {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private ManagementKeys() {
    }

    static String generate() {
        return ENCODER.encodeToString(randomBytes(32));
    }

    static String hash(String key) {
        byte[] salt = randomBytes(16);
        return ENCODER.encodeToString(salt) + "." + ENCODER.encodeToString(derive(key, salt));
    }

    static boolean matches(String key, String hash) {
        if (key == null || key.isBlank() || key.length() > 128 || hash == null) {
            return false;
        }
        String[] parts = hash.split("\\.", 2);
        if (parts.length != 2) {
            return false;
        }
        var decoder = Base64.getUrlDecoder();
        return MessageDigest.isEqual(decoder.decode(parts[1]), derive(key, decoder.decode(parts[0])));
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private static byte[] derive(String key, byte[] salt) {
        var spec = new PBEKeySpec(key.toCharArray(), salt, 600_000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No se pudo calcular el hash de gestión", exception);
        } finally {
            spec.clearPassword();
        }
    }
}
