package banking.auth.it;

import banking.auth.dto.requests.LoginRequest;
import banking.auth.dto.requests.LogoutRequest;
import banking.auth.dto.requests.RefreshRequest;
import banking.auth.dto.requests.RegisterRequest;
import banking.auth.repository.OutboxEventRepository;
import banking.auth.repository.SessionRepository;
import banking.auth.repository.UserRepository;
import banking.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AuthServiceIT extends IntegrationTestBase {
    @Autowired
    AuthService authService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SessionRepository sessionRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void cleanDb() {
        sessionRepository.deleteAll();
        outboxEventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void register_persistsUser_andCreatesSession() {
        long usersBefore = userRepository.count();
        long sessionsBefore = sessionRepository.count();
        long outboxBefore = outboxEventRepository.count();

        var response = authService.register(new RegisterRequest("login1234", "password123",
                "a@b.com"), "JUnit-UA");

        assertNotNull(response.getUserId());
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());

        assertEquals(usersBefore + 1, userRepository.count());
        assertEquals(sessionsBefore + 1, sessionRepository.count());
        assertEquals(outboxBefore + 1, outboxEventRepository.count(), "Expected 1 new outbox event");

        assertTrue(userRepository.findById(response.getUserId()).isPresent());
    }

    @Test
    public void login_createsNewSession() {
        authService.register(new RegisterRequest("login1234", "password123", "a@b.com"), "UA-1");
        long sessionsBefore = sessionRepository.count();
        long outboxBefore = outboxEventRepository.count();

        var response = authService.login(new LoginRequest("login1234", "password123"), "UA-2");

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());

        assertEquals(sessionsBefore + 1, sessionRepository.count());
        assertEquals(outboxBefore + 1, outboxEventRepository.count(), "Expected 1 new outbox event");
    }

    @Test
    public void refresh_returnsNewAccessToken() {
        var registered = authService.register(new RegisterRequest("login1234", "password123",
                "a@b.com"), "UA");
        var refreshed = authService.refresh(new RefreshRequest(registered.getRefreshToken()));

        assertNotNull(refreshed.getAccessToken());
    }

    @Test
    public void logout_deletesSession() {
        var registered = authService.register(new RegisterRequest("login1234", "password123",
                "a@b.com"), "UA");
        assertEquals(1, sessionRepository.count());

        authService.logout(new LogoutRequest(registered.getRefreshToken()));
        assertEquals(0, sessionRepository.count());
    }
}
