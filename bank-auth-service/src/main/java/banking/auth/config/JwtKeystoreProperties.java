package banking.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt.keystore")
@Getter
@Setter
public class JwtKeystoreProperties {
    private Resource path;
    private String password;
    private String alias;
    private String keypass;
}
