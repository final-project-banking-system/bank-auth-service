package banking.auth.unit;

import banking.auth.dto.requests.LoginRequest;
import banking.auth.dto.requests.LogoutRequest;
import banking.auth.dto.requests.RefreshRequest;
import banking.auth.dto.requests.RegisterRequest;
import banking.auth.dto.responses.LoginResponse;
import banking.auth.dto.responses.RefreshResponse;
import banking.auth.dto.responses.RegisterResponse;
import banking.auth.error.exception.ConflictException;
import banking.auth.error.exception.UnauthorizedException;
import banking.auth.model.entity.Session;
import banking.auth.model.entity.User;
import banking.auth.repository.SessionRepository;
import banking.auth.repository.UserRepository;
import banking.auth.service.AuthService;
import banking.auth.service.JwtTokenService;
import banking.auth.service.RefreshTokenService;
import banking.auth.service.publisher.AuthOutboxPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    UserRepository userRepository;

    @Mock
    SessionRepository sessionRepository;

    @Mock
    JwtTokenService jwtTokenService;

    @Mock
    RefreshTokenService refreshTokenService;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    AuthOutboxPublisher authOutboxPublisher;

    @InjectMocks
    AuthService authService;

    @BeforeEach
    void setupTopics() {
        ReflectionTestUtils.setField(authService, "topicUsers", "auth.users");
        ReflectionTestUtils.setField(authService, "topicLogins", "auth.logins");
    }

    @Test
    public void register_whenLoginExists_throwsConflict() {
        when(userRepository.existsByLogin("login")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("login", "pass1234",
                "a@b.com"), "ua")).isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
        verify(sessionRepository, never()).save(any());
        verify(authOutboxPublisher, never()).save(any(), any(), any(), any(), any());
    }

    @Test
    public void register_ok_savesUserSession_andOutbox() {
        var req = new RegisterRequest("login", "pass1234", "a@b.com");

        when(userRepository.existsByLogin("login")).thenReturn(false);
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("HASH");
        when(jwtTokenService.generateAccessToken(any(User.class))).thenReturn("ACCESS");
        when(refreshTokenService.generate()).thenReturn("REFRESH");
        when(refreshTokenService.toHash("REFRESH")).thenReturn("REFRESH_HASH");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });

        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);

        RegisterResponse response = authService.register(req, "UA");

        assertThat(response.getAccessToken()).isEqualTo("ACCESS");
        assertThat(response.getRefreshToken()).isEqualTo("REFRESH");
        assertThat(response.getUserId()).isNotNull();

        verify(sessionRepository).save(sessionCaptor.capture());
        Session savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getRefreshTokenHash()).isEqualTo("REFRESH_HASH");
        assertThat(savedSession.getDeviceInfo()).isEqualTo("UA");
        assertThat(savedSession.getExpiresAt()).isAfter(LocalDateTime.now());

        verify(authOutboxPublisher).save(eq("USER"), eq(response.getUserId()), eq("auth.users"), any(),
                eq("USER_CREATED"));
    }

    @Test
    public void login_whenAuthManagerThrows_throwsUnauthorized() {
        doThrow(new RuntimeException("bad creds")).when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(new LoginRequest("login", "bad"), "UA"))
                .isInstanceOf(UnauthorizedException.class);

        verify(userRepository, never()).findByLogin(anyString());
    }

    @Test
    public void refresh_whenTokenNotFound_throwsUnauthorized() {
        when(refreshTokenService.toHash("rt")).thenReturn("hash");
        when(sessionRepository.findByRefreshTokenHash("hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("rt")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    public void register_whenEmailExists_throwsConflict() {
        when(userRepository.existsByLogin("login")).thenReturn(false);
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("login", "pass1234", "a@b.com"), "UA"))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
        verify(sessionRepository, never()).save(any());
        verify(authOutboxPublisher, never()).save(any(), any(), any(), any(), any());
    }

    @Test
    public void login_ok_createsSession_andPublishesOutbox() {
        var req = new LoginRequest("login", "pass");
        var userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setLogin("login");

        when(userRepository.findByLogin("login")).thenReturn(Optional.of(user));

        when(jwtTokenService.generateAccessToken(eq(user))).thenReturn("ACCESS");
        when(refreshTokenService.generate()).thenReturn("REFRESH");
        when(refreshTokenService.toHash("REFRESH")).thenReturn("REFRESH_HASH");

        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);

        LoginResponse resp = authService.login(req, "UA-DEVICE");

        assertThat(resp.getAccessToken()).isEqualTo("ACCESS");
        assertThat(resp.getRefreshToken()).isEqualTo("REFRESH");

        verify(authenticationManager).authenticate(any());
        verify(sessionRepository).save(sessionCaptor.capture());

        Session saved = sessionCaptor.getValue();
        assertThat(saved.getUser()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(userId);
        assertThat(saved.getRefreshTokenHash()).isEqualTo("REFRESH_HASH");
        assertThat(saved.getDeviceInfo()).isEqualTo("UA-DEVICE");
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());

        verify(authOutboxPublisher).save(eq("USER"), eq(userId), eq("auth.logins"), any(),
                eq("USER_LOGIN"));
    }

    @Test
    public void login_whenUserNotFoundAfterAuth_throwsUnauthorized() {
        when(userRepository.findByLogin("login")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("login", "pass"), "UA"))
                .isInstanceOf(UnauthorizedException.class);

        verify(authenticationManager).authenticate(any());
        verify(sessionRepository, never()).save(any());
        verify(authOutboxPublisher, never()).save(any(), any(), any(), any(), any());
    }

    @Test
    public void refresh_whenExpired_deletesSession_andThrowsUnauthorized() {
        when(refreshTokenService.toHash("rt")).thenReturn("hash");

        User user = new User();
        user.setId(UUID.randomUUID());

        Session expired = new Session();
        expired.setId(UUID.randomUUID());
        expired.setUser(user);
        expired.setRefreshTokenHash("hash");
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(sessionRepository.findByRefreshTokenHash("hash")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("rt")))
                .isInstanceOf(UnauthorizedException.class);

        verify(sessionRepository).delete(eq(expired));
        verify(jwtTokenService, never()).generateAccessToken(any());
    }

    @Test
    public void refresh_ok_returnsNewAccessToken() {
        when(refreshTokenService.toHash("rt")).thenReturn("hash");

        User user = new User();
        user.setId(UUID.randomUUID());

        Session session = new Session();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setRefreshTokenHash("hash");
        session.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(sessionRepository.findByRefreshTokenHash("hash")).thenReturn(Optional.of(session));
        when(jwtTokenService.generateAccessToken(eq(user))).thenReturn("NEW_ACCESS");

        RefreshResponse response = authService.refresh(new RefreshRequest("rt"));

        assertThat(response.getAccessToken()).isEqualTo("NEW_ACCESS");
        verify(sessionRepository, never()).delete(any());
    }

    @Test
    public void logout_whenTokenNotFound_throwsUnauthorized() {
        when(refreshTokenService.toHash("rt")).thenReturn("hash");
        when(sessionRepository.findByRefreshTokenHash("hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout(new LogoutRequest("rt")))
                .isInstanceOf(UnauthorizedException.class);

        verify(sessionRepository, never()).delete(any());
    }

    @Test
    public void logout_ok_deletesSession() {
        when(refreshTokenService.toHash("rt")).thenReturn("hash");

        User user = new User();
        user.setId(UUID.randomUUID());

        Session session = new Session();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setRefreshTokenHash("hash");
        session.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(sessionRepository.findByRefreshTokenHash("hash")).thenReturn(Optional.of(session));

        authService.logout(new LogoutRequest("rt"));

        verify(sessionRepository).delete(eq(session));
    }
}
