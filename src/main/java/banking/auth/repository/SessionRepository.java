package banking.auth.repository;

import banking.auth.model.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByRefreshTokenHash(String refreshTokenHash);

    List<Session> findAllByUser_IdAndExpiresAtAfterOrderByCreatedAtDesc(UUID userId, LocalDateTime now);
}
