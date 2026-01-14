package banking.auth.web;

import banking.auth.config.SecurityConfig;
import banking.auth.controller.ProxyController;
import banking.auth.service.CustomUserDetailsService;
import banking.auth.service.ProxyService;
import banking.auth.service.publisher.SystemErrorPublisher;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProxyController.class)
@Import(SecurityConfig.class)
public class ProxyControllerWebTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    ProxyService proxyService;

    @MockitoBean
    SystemErrorPublisher systemErrorPublisher;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @Test
    public void proxy_isPublicEndpoint_returnsWhateverProxyServiceReturns() throws Exception {
        byte[] body = "OK".getBytes();
        var headers = new HttpHeaders();
        headers.add("X-Test", "1");

        when(proxyService.redirect(any()))
                .thenReturn(ResponseEntity.ok().headers(headers).body(body));

        mockMvc.perform(get("/proxy/core-banking/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Test", "1"))
                .andExpect(content().bytes(body));
    }
}
