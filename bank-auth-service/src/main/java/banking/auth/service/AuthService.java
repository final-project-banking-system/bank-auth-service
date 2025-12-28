package banking.auth.service;

import banking.auth.dto.kafka.UserCreatedEvent;
import banking.auth.dto.kafka.UserLoginEvent;
import banking.auth.dto.requests.LoginRequest;
import banking.auth.dto.requests.LogoutRequest;
import banking.auth.dto.requests.RefreshRequest;
import banking.auth.dto.requests.RegisterRequest;
import banking.auth.dto.responses.LoginResponse;
import banking.auth.dto.responses.RefreshResponse;
import banking.auth.dto.responses.RegisterResponse;
import banking.auth.dto.responses.SessionResponse;
import banking.auth.error.exception.ConflictException;
import banking.auth.error.exception.ForbiddenException;
import banking.auth.error.exception.NotFoundException;
import banking.auth.error.exception.UnauthorizedException;
import banking.auth.model.entity.Session;
import banking.auth.model.entity.User;
import banking.auth.repository.SessionRepository;
import banking.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final static long REFRESH_TOKEN_TTL = 7;

    @Transactional
    public RegisterResponse register(RegisterRequest request, String deviceInfo) {
        var login = request.getLogin();
        var email = request.getEmail();
        var password = request.getPassword();

        log.info("Register attempt: login={}, email={}", login, email);

        if (userRepository.existsByLogin(login)) {
            throw new ConflictException("This login already belongs to other user: " + login);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("This email already belongs to other user: " + email);
        }

        User userEntity = User.builder()
                .login(login)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .build();

        User savedUser = userRepository.save(userEntity);

        String accessToken = jwtTokenService.generateAccessToken(savedUser);
        String refreshToken = refreshTokenService.generate();
        String refreshTokenHash = refreshTokenService.toHash(refreshToken);

        Session session = Session.builder()
                .refreshTokenHash(refreshTokenHash)
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_TTL))
                .deviceInfo(normalizeDeviceInfo(deviceInfo))
                .build();
        savedUser.addSession(session);
        sessionRepository.save(session);

        UserCreatedEvent event = new UserCreatedEvent(
                savedUser.getId(),
                savedUser.getLogin(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
        kafkaTemplate.send("auth.users", savedUser.getId().toString(), event);

        log.info("Register success: userId={}, sessionId={}", savedUser.getId(), session.getId());

        return new RegisterResponse(savedUser.getId(), accessToken, refreshToken);
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String deviceInfo) {
        String login = request.getLogin();
        log.info("Login attempt: login={}", login);

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(login, request.getPassword()));
        } catch (Exception e) {
            log.warn("Login failed: invalid credentials. login={}", login);
            throw new UnauthorizedException("Invalid login or password");
        }

        User user = userRepository.findByLogin(request.getLogin()).orElseThrow(() -> {
            log.warn("Login failed: user not found after auth. login={}", login);
            return new UnauthorizedException("Invalid login");
        });

        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = refreshTokenService.generate();
        String refreshTokenHash = refreshTokenService.toHash(refreshToken);

        Session session = Session.builder()
                .refreshTokenHash(refreshTokenHash)
                .deviceInfo(normalizeDeviceInfo(deviceInfo))
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_TTL))
                .build();
        user.addSession(session);
        sessionRepository.save(session);

        UserLoginEvent event = new UserLoginEvent(
                user.getId(),
                user.getLogin(),
                normalizeDeviceInfo(deviceInfo),
                LocalDateTime.now()
        );
        kafkaTemplate.send("auth.logins", user.getId().toString(), event);

        log.info("Login success: userId={}, sessionId={}", user.getId(), session.getId());

        return new LoginResponse(accessToken, refreshToken);
    }

    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        String requestRefreshTokenHash = refreshTokenService.toHash(request.getRefreshToken());

        log.info("Refresh attempt");

        Session session = sessionRepository.findByRefreshTokenHash(requestRefreshTokenHash).orElseThrow(() -> {
            log.warn("Refresh failed: token not found");
            return new UnauthorizedException("Invalid refresh token");
        });

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Refresh failed: token expired. userId={}, sessionId={}", session.getUser().getId(), session.getId());
            sessionRepository.delete(session);
            throw new UnauthorizedException("Refresh token expired");
        }
        String accessToken = jwtTokenService.generateAccessToken(session.getUser());

        log.info("Refresh success: userId={}, sessionId={}", session.getUser().getId(), session.getId());

        return new RefreshResponse(accessToken);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String requestRefreshTokenHash = refreshTokenService.toHash(request.getRefreshToken());

        log.info("Logout attempt");

        Session session = sessionRepository.findByRefreshTokenHash(requestRefreshTokenHash).orElseThrow(() -> {
            log.warn("Logout failed: refresh token not found");
            return new UnauthorizedException("Invalid refresh token");
        });

        log.info("Logout success: userId={}, sessionId={}", session.getUser().getId(), session.getId());

        sessionRepository.delete(session);
    }

    public List<SessionResponse> listOfSessions(UUID userId) {
        log.info("List sessions: userId={}", userId);

        var result = sessionRepository.findAllByUser_Id(userId).stream()
                .filter(session -> session.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(session -> {
                    SessionResponse sessionResponse = new SessionResponse();
                    sessionResponse.setSessionId(session.getId());
                    sessionResponse.setCreatedAt(session.getCreatedAt());
                    sessionResponse.setDeviceInfo(session.getDeviceInfo());
                    sessionResponse.setExpiresAt(session.getExpiresAt());
                    return sessionResponse;
                })
                .toList();

        log.info("List sessions result: userId={}, activeCount={}", userId, result.size());

        return result;
    }

    @Transactional
    public void revokeSession(UUID userId, UUID sessionId) {
        log.info("Revoke session attempt: requesterUserId={}, sessionId={}", userId, sessionId);

        Session session = sessionRepository.findById(sessionId).orElseThrow(() -> {
            log.warn("Revoke session failed: not found. sessionId={}", sessionId);
            return new NotFoundException("Session not found");
        });

        if (!session.getUser().getId().equals(userId)) {
            log.warn("Revoke session forbidden: requesterUserId={}, ownerUserId={}, sessionId={}",
                    userId, session.getUser().getId(), sessionId);
            throw new ForbiddenException("Session belongs to another user");
        }

        log.info("Revoke session success: userId={}, sessionId={}", userId, sessionId);

        sessionRepository.delete(session);
    }

    private String normalizeDeviceInfo(String rawData) {
        if (rawData == null || rawData.isBlank()) return "unknown";
        rawData = rawData.trim();
        int max = 512;
        return rawData.length() <= max ? rawData : rawData.substring(0, max);
    }
}
