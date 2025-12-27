package banking.auth.config;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class ProxyClientConfig {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(10))
                .withReadTimeout(Duration.ofSeconds(20));
        ClientHttpRequestFactory httpRequestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        return builder.requestFactory(() -> httpRequestFactory).build();
    }
}
