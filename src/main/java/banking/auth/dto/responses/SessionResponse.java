package banking.auth.dto.responses;

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
public class SessionResponse {
    private UUID sessionId;
    private String deviceInfo;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
