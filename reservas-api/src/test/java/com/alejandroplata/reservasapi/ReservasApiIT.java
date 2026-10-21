package com.alejandroplata.reservasapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Prueba la API de punta a punta contra un PostgreSQL real (Testcontainers). Requiere Docker. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
        "app.jwt.secret=un-secreto-de-pruebas-con-mas-de-32-caracteres",
        "app.admin.email=admin@test.com",
        "app.admin.password=admin-de-pruebas"
})
class ReservasApiIT {

    // static: un único contenedor para todos los tests de la clase (arrancar Postgres es lo lento)
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    // ---------- helpers ----------

    private String token(String path, String email, String password) throws Exception {
        String body = json.writeValueAsString(java.util.Map.of("email", email, "password", password));
        String res = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("token").asText();
    }

    private String nuevoUsuario() throws Exception {
        return token("/api/auth/register", "user-" + UUID.randomUUID() + "@test.com", "password-123");
    }

    private String admin() throws Exception {
        return token("/api/auth/login", "admin@test.com", "admin-de-pruebas");
    }

    private long nuevoRecurso(String adminToken) throws Exception {
        String body = "{\"name\":\"Sala " + UUID.randomUUID() + "\",\"description\":\"d\",\"category\":\"SALA\",\"capacity\":4}";
        String res = mvc.perform(post("/api/resources").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("id").asLong();
    }

    private String reserva(long resourceId, Instant start, Instant end) {
        return "{\"resourceId\":" + resourceId + ",\"startAt\":\"" + start + "\",\"endAt\":\"" + end + "\"}";
    }

    private Instant manana(int hora) {
        return Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(hora, ChronoUnit.HOURS);
    }

    // ---------- autenticación y roles ----------

    @Test
    @DisplayName("sin token recibes 401 con cuerpo JSON de error")
    void sinTokenEs401() throws Exception {
        mvc.perform(get("/api/resources")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").exists()).andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("registro, login y acceso con el token")
    void registroLoginYAcceso() throws Exception {
        String email = "flujo-" + UUID.randomUUID() + "@test.com";
        String body = "{\"email\":\"" + email + "\",\"password\":\"password-123\"}";

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("USER"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        String token = token("/api/auth/login", email, "password-123");
        mvc.perform(get("/api/resources").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        String mala = "{\"email\":\"" + email + "\",\"password\":\"incorrecta-1\"}";
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(mala))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("el registro valida los campos y dice cuál falló")
    void registroInvalido() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"no-es-email\",\"password\":\"corta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("un usuario normal recibe 403 al gestionar recursos o listar todas las reservas")
    void usuarioNoEsAdmin() throws Exception {
        String user = nuevoUsuario();
        mvc.perform(post("/api/resources").header("Authorization", "Bearer " + user)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\",\"capacity\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/reservations").header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden());
    }

    // ---------- recursos ----------

    @Test
    @DisplayName("el admin crea, edita y desactiva un recurso; el JSON de error de validación es coherente")
    void crudDeRecursos() throws Exception {
        String admin = admin();
        long id = nuevoRecurso(admin);

        mvc.perform(post("/api/resources").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"capacity\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.capacity").exists());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/resources/" + id)
                .header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renombrada\",\"description\":\"\",\"category\":\"PISTA\",\"capacity\":10}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Renombrada"));

        mvc.perform(delete("/api/resources/" + id).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/resources/" + id).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/resources/999999").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("el catálogo se puede filtrar por categoría")
    void filtraPorCategoria() throws Exception {
        String admin = admin();
        String nombre = "Pista " + UUID.randomUUID();
        mvc.perform(post("/api/resources").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + nombre + "\",\"category\":\"PISTA\",\"capacity\":4}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.category").value("PISTA"));

        mvc.perform(get("/api/resources?category=PISTA&name=" + nombre).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/resources?category=EQUIPO&name=" + nombre).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/resources?category=INVENTADA").header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("el tamaño de página tiene un máximo")
    void paginaConMaximo() throws Exception {
        mvc.perform(get("/api/resources?size=1000").header("Authorization", "Bearer " + nuevoUsuario()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50));
    }

    // ---------- reservas ----------

    @Test
    @DisplayName("reservar, ver disponibilidad, intentar solapar, cancelar y reservar de nuevo el hueco liberado")
    void flujoDeReservas() throws Exception {
        String admin = admin();
        String ana = nuevoUsuario();
        String luis = nuevoUsuario();
        long recurso = nuevoRecurso(admin);
        Instant inicio = manana(10);

        String creada = mvc.perform(post("/api/reservations").header("Authorization", "Bearer " + ana)
                .contentType(MediaType.APPLICATION_JSON).content(reserva(recurso, inicio, inicio.plus(1, ChronoUnit.HOURS))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn().getResponse().getContentAsString();
        long reservaId = json.readTree(creada).get("id").asLong();

        // solapada (empieza dentro de la anterior) -> 409
        mvc.perform(post("/api/reservations").header("Authorization", "Bearer " + luis)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reserva(recurso, inicio.plus(30, ChronoUnit.MINUTES), inicio.plus(90, ChronoUnit.MINUTES))))
                .andExpect(status().isConflict());

        // contigua (empieza justo cuando acaba la otra) -> permitida
        mvc.perform(post("/api/reservations").header("Authorization", "Bearer " + luis)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reserva(recurso, inicio.plus(1, ChronoUnit.HOURS), inicio.plus(2, ChronoUnit.HOURS))))
                .andExpect(status().isCreated());

        // disponibilidad: dos tramos ocupados ese día
        String dia = inicio.toString().substring(0, 10);
        mvc.perform(get("/api/resources/" + recurso + "/availability?date=" + dia)
                .header("Authorization", "Bearer " + ana))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));

        // Luis no puede cancelar la reserva de Ana; Ana sí
        mvc.perform(patch("/api/reservations/" + reservaId + "/cancel").header("Authorization", "Bearer " + luis))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/reservations/" + reservaId + "/cancel").header("Authorization", "Bearer " + ana))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));

        // el hueco liberado vuelve a estar disponible
        mvc.perform(post("/api/reservations").header("Authorization", "Bearer " + luis)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reserva(recurso, inicio, inicio.plus(1, ChronoUnit.HOURS))))
                .andExpect(status().isCreated());

        // Ana solo ve las suyas (1, la cancelada); el admin filtra por recurso y estado
        mvc.perform(get("/api/reservations/me").header("Authorization", "Bearer " + ana))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/reservations?resourceId=" + recurso + "&status=CONFIRMED")
                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("las reglas de negocio devuelven 422 y los recursos inactivos no se reservan")
    void reglasDeNegocio() throws Exception {
        String admin = admin();
        String user = nuevoUsuario();
        long recurso = nuevoRecurso(admin);

        // pasado
        mvc.perform(post("/api/reservations").header("Authorization", "Bearer " + user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reserva(recurso, Instant.now().minus(2, ChronoUnit.DAYS), Instant.now().minus(47, ChronoUnit.HOURS))))
                .andExpect(status().isUnprocessableEntity());

        // inactivo
        mvc.perform(delete("/api/resources/" + recurso).header("Authorization", "Bearer " + admin));
        mvc.perform(post("/api/reservations").header("Authorization", "Bearer " + user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reserva(recurso, manana(10), manana(11))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("con peticiones simultáneas al mismo hueco, solo una reserva se confirma")
    void sinDobleReservaConcurrente() throws Exception {
        String admin = admin();
        long recurso = nuevoRecurso(admin);
        Instant inicio = manana(12);
        String body = reserva(recurso, inicio, inicio.plus(1, ChronoUnit.HOURS));

        int hilos = 10;
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < hilos; i++) tokens.add(nuevoUsuario());

        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1); // todos esperan la señal para disparar a la vez
        List<Future<Integer>> resultados = new ArrayList<>();
        for (String t : tokens) {
            Callable<Integer> peticion = () -> {
                salida.await();
                return mvc.perform(post("/api/reservations").header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andReturn().getResponse().getStatus();
            };
            resultados.add(pool.submit(peticion));
        }
        salida.countDown();

        int creadas = 0, conflictos = 0;
        for (Future<Integer> f : resultados) {
            int codigo = f.get();
            if (codigo == 201) creadas++;
            else if (codigo == 409) conflictos++;
        }
        pool.shutdown();

        assertThat(creadas).isEqualTo(1);
        assertThat(conflictos).isEqualTo(hilos - 1);

        JsonNode lista = json.readTree(mvc.perform(get("/api/reservations?resourceId=" + recurso + "&status=CONFIRMED")
                .header("Authorization", "Bearer " + admin)).andReturn().getResponse().getContentAsString());
        assertThat(lista.get("totalElements").asInt()).isEqualTo(1);
    }
}
