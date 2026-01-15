package banking.auth.web;

import banking.auth.config.SecurityConfig;
import banking.auth.controller.AuthController;
import banking.auth.dto.requests.LoginRequest;
import banking.auth.dto.requests.LogoutRequest;
import banking.auth.dto.requests.RefreshRequest;
import banking.auth.dto.requests.RegisterRequest;
import banking.auth.dto.responses.LoginResponse;
import banking.auth.dto.responses.RefreshResponse;
import banking.auth.dto.responses.RegisterResponse;
import banking.auth.dto.responses.SessionResponse;
import banking.auth.service.AuthService;
import banking.auth.service.CustomUserDetailsService;
import banking.auth.service.publisher.SystemErrorPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
public class AuthControllerWebTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    SystemErrorPublisher systemErrorPublisher;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @Test
    public void register_validationFails_returns400() throws Exception {
        var body = new RegisterRequest("", "", "not-an-email");

        mockMvc.perform(post("/auth/register").contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    public void register_ok_returns201_andBody() throws Exception {
        UUID userId = UUID.randomUUID();
        var body = new RegisterRequest("login1234", "password123", "a@b.com");

        var response = new RegisterResponse(userId, "access", "refresh");
        when(authService.register(any(RegisterRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/auth/register").header("User-Agent", "JUnit")
                        .contentType("application/json").content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"));
    }

    @Test
    public void login_ok_returns200() throws Exception {
        var body = new LoginRequest("login1234", "password123");
        when(authService.login(any(LoginRequest.class), any()))
                .thenReturn(new LoginResponse("access", "refresh"));

        mockMvc.perform(post("/auth/login").header("User-Agent", "JUnit")
                        .contentType("application/json").content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"));
    }

    @Test
    public void refresh_ok_returns200() throws Exception {
        var body = new RefreshRequest("refreshToken");
        when(authService.refresh(any(RefreshRequest.class))).thenReturn(new RefreshResponse("newAccess"));

        mockMvc.perform(post("/auth/refresh").contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("newAccess"));
    }

    @Test
    public void logout_ok_returns204() throws Exception {
        var body = new LogoutRequest("refreshToken");

        mockMvc.perform(post("/auth/logout").contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());

        verify(authService).logout(any(LogoutRequest.class));
    }

    @Test
    public void sessions_requiresAuth_401() throws Exception {
        mockMvc.perform(get("/auth/sessions")).andExpect(status().isUnauthorized());
    }

    @Test
    public void sessions_withJwt_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authService.listOfSessions(eq(userId))).thenReturn(List.of(new SessionResponse(UUID.randomUUID(),
                "dev", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusDays(1))));

        mockMvc.perform(get("/auth/sessions").with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].deviceInfo").value("dev"));
    }

    @Test
    public void revoke_requiresAuth_401() throws Exception {
        mockMvc.perform(post("/auth/sessions/" + UUID.randomUUID() + "/revoke")).andExpect(status()
                .isUnauthorized());
    }

    @Test
    public void revoke_withJwt_returns204() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(post("/auth/sessions/" + sessionId + "/revoke")
                .with(jwt().jwt(j -> j.subject(userId.toString())))).andExpect(status().isNoContent());

        verify(authService).revokeSession(userId, sessionId);
    }
}
