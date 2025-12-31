package banking.auth.repository;

import banking.auth.model.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByRefreshTokenHash(String refreshTokenHash);

    List<Session> findAllByUser_Id(UUID userId);
}
