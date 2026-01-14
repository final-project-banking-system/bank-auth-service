package banking.auth.web;

import banking.auth.config.SecurityConfig;
import banking.auth.controller.JwksController;
import banking.auth.service.CustomUserDetailsService;
import banking.auth.service.publisher.SystemErrorPublisher;
import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = JwksController.class)
@Import(SecurityConfig.class)
public class JwksControllerWebTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    JWKSet jwkSet;

    @MockitoBean
    SystemErrorPublisher systemErrorPublisher;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @Test
    public void jwks_returns200_andJson() throws Exception {
        when(jwkSet.toPublicJWKSet()).thenReturn(jwkSet);
        when(jwkSet.toJSONObject()).thenReturn(Map.of("keys", List.of()));

        mockMvc.perform(get("/auth/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").exists());
    }
}
