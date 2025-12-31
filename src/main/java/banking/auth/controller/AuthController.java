package banking.auth.controller;

import banking.auth.dto.requests.LoginRequest;
import banking.auth.dto.requests.LogoutRequest;
import banking.auth.dto.requests.RefreshRequest;
import banking.auth.dto.requests.RegisterRequest;
import banking.auth.dto.responses.LoginResponse;
import banking.auth.dto.responses.RefreshResponse;
import banking.auth.dto.responses.RegisterResponse;
import banking.auth.dto.responses.SessionResponse;
import banking.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request,
                                                     @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        RegisterResponse response = authService.register(request, userAgent);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        LoginResponse response = authService.login(request, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> sessions(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(authService.listOfSessions(userId));
    }

    @PostMapping("/sessions/{sessionId}/revoke")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        authService.revokeSession(userId, sessionId);
        return ResponseEntity.noContent().build();
    }
}
