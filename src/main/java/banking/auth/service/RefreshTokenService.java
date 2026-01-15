package banking.auth.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Service
public class RefreshTokenService {
    public String generate() {
        return UUID.randomUUID().toString();
    }

    public String toHash(String refreshToken) {
        try {
            MessageDigest instance = MessageDigest.getInstance("SHA-256");
            byte[] digest = instance.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hashRefreshToken = new StringBuilder();
            for (byte oneByte : digest) {
                hashRefreshToken.append(String.format("%02x", oneByte));
            }
            return hashRefreshToken.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash refresh token", e);
        }
    }
}
