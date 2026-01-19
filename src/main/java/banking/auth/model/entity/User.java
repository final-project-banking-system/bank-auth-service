package banking.auth.model.entity;

import banking.auth.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {
    @Column(name = "login", nullable = false, unique = true, length = 32)
    private String login;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

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
}
