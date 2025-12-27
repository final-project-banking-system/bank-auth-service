package banking.auth.model.entity;

import banking.auth.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @PrePersist
    private void generateId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    @Column(name = "login", nullable = false, unique = true, length = 50)
    private String login;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Session> sessions = new ArrayList<>();

    public List<Session> getSessions() {
        return Collections.unmodifiableList(sessions);
    }

    public void addSession(Session session) {
        if (session == null) return;

        boolean exists = sessions.stream().anyMatch(s ->
                s == session || (s.getId() != null && s.getId().equals(session.getId()))
        );
        if (exists) return;

        sessions.add(session);
        session.setUser(this);
    }

    public void removeSession(Session session) {
        if (session == null) return;
        sessions.remove(session);
        session.setUser(null);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof User)) return false;
        User that = (User) obj;
        return id != null && id.equals(that.id);
    }
}
