package banking.auth.dto.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank
    @Size(min = 4, max = 32)
    private String login;

    @NotBlank
    @Size(min = 6, max = 50)
    private String password;

    @NotBlank
    @Email
    private String email;
}
