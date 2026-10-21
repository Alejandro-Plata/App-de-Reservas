package com.alejandroplata.reservasapi.user;

import com.alejandroplata.reservasapi.config.AdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Registrarse solo crea usuarios normales, así que el primer admin sale de la configuración
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AdminProperties props;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(AdminProperties props, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.props = props;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (props.email() == null || props.email().isBlank() || props.password() == null || props.password().isBlank()) {
            return;
        }
        String email = AuthService.normalizar(props.email());
        if (!userRepository.existsByEmail(email)) {
            userRepository.save(new User(email, passwordEncoder.encode(props.password()), Role.ADMIN));
            log.info("Administrador inicial creado: {}", email);
        }
    }
}
