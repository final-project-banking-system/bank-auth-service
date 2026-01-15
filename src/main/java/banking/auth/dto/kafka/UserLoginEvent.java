package banking.auth.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginEvent {
    private UUID userId;
    private String login;
    private String deviceInfo;
    private LocalDateTime occurredAt;
}
