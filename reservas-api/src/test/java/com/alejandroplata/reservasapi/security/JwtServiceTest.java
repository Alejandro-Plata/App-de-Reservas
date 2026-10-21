package com.alejandroplata.reservasapi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alejandroplata.reservasapi.config.JwtProperties;
import com.alejandroplata.reservasapi.user.Role;
import com.alejandroplata.reservasapi.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static final String SECRET = "un-secreto-de-prueba-con-mas-de-32-caracteres";

    private User usuario() {
        User u = new User("ana@correo.com", "hash", Role.ADMIN);
        ReflectionTestUtils.setField(u, "id", 7L);
        return u;
    }

    @Test
    @DisplayName("un token generado se puede leer de vuelta con id, e-mail y rol")
    void generaYLeeToken() {
        var jwt = new JwtService(new JwtProperties(SECRET, 60));

        var principal = jwt.parse(jwt.generate(usuario()));

        assertThat(principal).contains(new UserPrincipal(7L, "ana@correo.com", Role.ADMIN));
    }

    @Test
    @DisplayName("un token firmado con otra clave se rechaza")
    void rechazaFirmaAjena() {
        var emisor = new JwtService(new JwtProperties("otro-secreto-distinto-con-mas-de-32-caracteres", 60));
        var receptor = new JwtService(new JwtProperties(SECRET, 60));

        assertThat(receptor.parse(emisor.generate(usuario()))).isEmpty();
    }

    @Test
    @DisplayName("un token caducado se rechaza")
    void rechazaCaducado() {
        var jwt = new JwtService(new JwtProperties(SECRET, -1));

        assertThat(jwt.parse(jwt.generate(usuario()))).isEmpty();
    }

    @Test
    @DisplayName("un texto que no es un token se rechaza sin lanzar excepción")
    void rechazaBasura() {
        assertThat(new JwtService(new JwtProperties(SECRET, 60)).parse("esto-no-es-un-jwt")).isEmpty();
    }

    @Test
    @DisplayName("no arranca con un secreto demasiado corto")
    void rechazaSecretoCorto() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("corto", 60)))
                .isInstanceOf(IllegalStateException.class);
    }
}
