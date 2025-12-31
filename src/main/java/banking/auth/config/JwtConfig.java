package banking.auth.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtConfig {
    @Bean
    public RSAKey rsaKey(JwtKeystoreProperties properties) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream inputStream = properties.getPath().getInputStream()) {
                keyStore.load(inputStream, properties.getPassword().toCharArray());
            }

            RSAPublicKey publicKey = (RSAPublicKey) keyStore.getCertificate(properties.getAlias()).getPublicKey();
            Key key = keyStore.getKey(properties.getAlias(), properties.getKeypass().toCharArray());
            RSAPrivateKey privateKey = (RSAPrivateKey) key;

            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(properties.getAlias())
                    .build();

        } catch (Exception e) {
            throw new IllegalStateException("Cannot load RSA keys from keystore", e);
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public JWKSet jwkSet(RSAKey rsaKey) {
        return new JWKSet(rsaKey);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey) throws JOSEException {
        return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
    }
}
