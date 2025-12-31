package banking.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.proxy")
@Getter
@Setter
public class ProxyRoutesProperties {
    private Map<String, String> routes = new HashMap<>();
}
