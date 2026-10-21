package com.alejandroplata.reservasapi.security;

import com.alejandroplata.reservasapi.config.JwtProperties;
import com.alejandroplata.reservasapi.user.Role;
import com.alejandroplata.reservasapi.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(JwtProperties props) {
        byte[] secret = props.secret() == null ? new byte[0] : props.secret().getBytes(StandardCharsets.UTF_8);
        // HS256 exige al menos 256 bits; mejor fallar al arrancar que firmar con una clave débil
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.expiration = Duration.ofMinutes(props.expirationMinutes());
    }

    public String generate(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /** Devuelve el usuario del token, o vacío si está mal firmado, caducado o malformado. */
    public Optional<UserPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Long id = ((Number) claims.get("uid")).longValue();
            Role role = Role.valueOf(claims.get("role", String.class));
            return Optional.of(new UserPrincipal(id, claims.getSubject(), role));
        } catch (JwtException | IllegalArgumentException | NullPointerException | ClassCastException e) {
            return Optional.empty();
        }
    }
}
