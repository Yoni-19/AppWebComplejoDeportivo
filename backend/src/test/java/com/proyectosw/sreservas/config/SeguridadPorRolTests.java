package com.proyectosw.sreservas.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Spring Boot 4 usa Jackson 3: el ObjectMapper autoconfigurado vive en tools.jackson.
import tools.jackson.databind.ObjectMapper;

/**
 * Verifica la matriz de permisos de SecurityConfig contra la API real.
 * Usa los usuarios del seed: admin@email.com (ADMIN) y laura@email.com (CLIENTE).
 * Requiere MySQL arriba, igual que el resto de los tests del proyecto.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadPorRolTests {

    private static final String CLAVE_SEED = "test123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void sinTokenLasRutasPrivadasDevuelven401() throws Exception {
        mockMvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void elRegistroEsPublicoYLaSeguridadNoLoFrena() throws Exception {
        // Cuerpo vacio: llega al controller y falla la validacion (400), no la seguridad.
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unTokenInvalidoNoBloqueaRutasPublicasPeroSiLasPrivadas() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("Authorization", "Bearer token-invalido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"admin@email.com\",\"clave\":\"test123\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer token-invalido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elClienteConsultaCanchasYReservasPeroNoUsuariosNiReservasAjenas() throws Exception {
        String token = tokenDe("laura@email.com", CLAVE_SEED);

        mockMvc.perform(get("/api/canchas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/reservas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        // La reserva 2 del seed pertenece a otro usuario: reprogramarla da 403, no 400 ni 200.
        mockMvc.perform(put("/api/reservas/2").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fechaHoraInicio":"2026-11-03T09:00:00","fechaHoraFin":"2026-11-03T10:30:00","canchaId":1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void elPerfilPropioDevuelveElRolDeLaBase() throws Exception {
        String token = tokenDe("laura@email.com", CLAVE_SEED);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("laura@email.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    void elAdminGestionaUsuariosYRoles() throws Exception {
        String token = tokenDe("admin@email.com", CLAVE_SEED);

        String cuerpo = mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // El admin no se ve a sí mismo: su cuenta se edita desde "Mi perfil".
        assertThat(mapper.readTree(cuerpo).findValuesAsString("correo"))
                .contains("laura@email.com", "pedro@email.com")
                .doesNotContain("admin@email.com");
    }

    @Test
    void elAdminNoPuedeCambiarSuRolNiEliminarsePeroSiConsultarSuPerfil() throws Exception {
        String token = tokenDe("admin@email.com", CLAVE_SEED);
        String me = mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int id = mapper.readTree(me).get("id").asInt();

        mockMvc.perform(put("/api/usuarios/" + id + "/rol")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\":\"CLIENTE\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/usuarios/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/usuarios/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("admin@email.com"));
    }

    @Test
    void elRecepcionistaRegistraYEditaClientesPeroNoGestionaPersonalNiRoles() throws Exception {
        String admin = tokenDe("admin@email.com", CLAVE_SEED);
        String correo = "prueba.recepcionista." + UUID.randomUUID() + "@test.local";
        int adminId = mapper.readTree(
                mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + admin))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
                .get("id").asInt();

        String alta = mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombres":"Prueba","apellidos":"Recepcionista","correo":"%s","clave":"test123"}
                                """.formatted(correo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = mapper.readTree(alta).get("id").asInt();
        int clienteId = -1;

        try {
            mockMvc.perform(put("/api/usuarios/" + id + "/rol")
                            .header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rol\":\"RECEPCIONISTA\"}"))
                    .andExpect(status().isOk());

            String recepcionista = tokenDe(correo, CLAVE_SEED);

            // El recepcionista puede registrar un cliente presencial.
            String clienteCorreo = "prueba.cliente." + UUID.randomUUID() + "@test.local";
            String altaCliente = mockMvc.perform(post("/api/usuarios")
                            .header("Authorization", "Bearer " + recepcionista)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombres":"Cliente","apellidos":"Presencial","correo":"%s","clave":"test123"}
                                    """.formatted(clienteCorreo)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.rol").value("CLIENTE"))
                    .andReturn().getResponse().getContentAsString();
            clienteId = mapper.readTree(altaCliente).get("id").asInt();

            // Y puede corregir sus datos.
            mockMvc.perform(put("/api/usuarios/" + clienteId)
                            .header("Authorization", "Bearer " + recepcionista)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombres":"Cliente Editado","apellidos":"Presencial","correo":"%s"}
                                    """.formatted(clienteCorreo)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombres").value("Cliente Editado"));

            // Pero no puede editar al personal ni a sí mismo.
            mockMvc.perform(put("/api/usuarios/" + adminId)
                            .header("Authorization", "Bearer " + recepcionista)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombres":"Admin","apellidos":"Test","correo":"admin@email.com"}
                                    """))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put("/api/usuarios/" + id)
                            .header("Authorization", "Bearer " + recepcionista)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombres":"Prueba","apellidos":"Recepcionista","correo":"%s"}
                                    """.formatted(correo)))
                    .andExpect(status().isForbidden());

            String comoRecepcionista = mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + recepcionista))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            var lista = mapper.readTree(comoRecepcionista);
            // Solo clientes: ni él mismo, ni el admin.
            assertThat(lista.findValuesAsString("rol")).isNotEmpty().containsOnly("CLIENTE");
            assertThat(lista.findValuesAsString("correo"))
                    .contains(clienteCorreo)
                    .doesNotContain(correo, "admin@email.com");

            // Aunque pida otro rol, el backend le sigue devolviendo solo clientes.
            String filtrado = mockMvc.perform(get("/api/usuarios?rol=ADMIN")
                            .header("Authorization", "Bearer " + recepcionista))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(mapper.readTree(filtrado).findValuesAsString("correo"))
                    .doesNotContain(correo, "admin@email.com");

            // El admin sí ve al recepcionista (para poder cambiarle el rol), pero no a sí mismo.
            String comoAdmin = mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + admin))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(mapper.readTree(comoAdmin).findValuesAsString("correo"))
                    .contains(correo)
                    .doesNotContain("admin@email.com");

            mockMvc.perform(put("/api/usuarios/" + id + "/rol")
                            .header("Authorization", "Bearer " + recepcionista)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rol\":\"CLIENTE\"}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/usuarios/" + clienteId)
                            .header("Authorization", "Bearer " + recepcionista))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/canchas")
                            .header("Authorization", "Bearer " + recepcionista)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        } finally {
            if (clienteId > 0) {
                mockMvc.perform(delete("/api/usuarios/" + clienteId)
                        .header("Authorization", "Bearer " + admin));
            }
            mockMvc.perform(delete("/api/usuarios/" + id).header("Authorization", "Bearer " + admin));
        }
    }

    /** Hace login real y devuelve el JWT; asi se prueba tambien que el rol sale de la base. */
    private String tokenDe(String correo, String clave) throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"%s\",\"clave\":\"%s\"}".formatted(correo, clave)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(respuesta).get("token").asText();
    }
}
