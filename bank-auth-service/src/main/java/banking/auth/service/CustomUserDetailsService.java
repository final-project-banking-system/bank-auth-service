package banking.auth.service;

import banking.auth.model.entity.User;
import banking.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = userRepository.findByLogin(login).orElseThrow(() ->
                new UsernameNotFoundException("User not found: " + login));

        return org.springframework.security.core.userdetails.User.withUsername(user.getLogin())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }
}
