package com.irisperez.tfg.encuentraformacion.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET = "claveSecretaParaTestsDeLaAplicacionFP2024XYZ";
    private static final long DURACION = 86_400_000L;

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Inicializa el servicio e inyecta propiedades privadas usando ReflectionTestUtils para simular @Value
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "key", SECRET);
        ReflectionTestUtils.setField(jwtService, "duracion", DURACION);

        userDetails = new User(
                "ana@test.com",
                "$2a$hashed",
                List.of(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"))
        );
    }

    @Nested
    @DisplayName("generarTokenBasico()")
    class GenerarToken {

        @Test
        @DisplayName("retorna un token no nulo ni vacío")
        void retornaTokenNoNuloNiVacio() {
            String token = jwtService.generarTokenBasico(userDetails);
            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("el token tiene formato JWT con tres partes separadas por punto")
        void tokenTieneFormatoJwt() {
            String token = jwtService.generarTokenBasico(userDetails);
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("el subject del token es el email del usuario")
        void subjectEsEmailDelUsuario() {
            String token = jwtService.generarTokenBasico(userDetails);
            assertThat(jwtService.extraerEmail(token)).isEqualTo("ana@test.com");
        }
    }

    @Nested
    @DisplayName("extraerEmail()")
    class ExtraerEmail {

        @Test
        @DisplayName("token válido retorna el email correcto")
        void tokenValido_retornaEmail() {
            String token = jwtService.generarTokenBasico(userDetails);
            assertThat(jwtService.extraerEmail(token)).isEqualTo("ana@test.com");
        }

        @Test
        @DisplayName("token manipulado lanza excepción JWT")
        void tokenManipulado_lanzaExcepcion() {
            String tokenInvalido = "eyJhbGciOiJIUzI1NiJ9.MANIPULADO.firma";
            assertThatThrownBy(() -> jwtService.extraerEmail(tokenInvalido))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValid {

        @Test
        @DisplayName("token del mismo usuario retorna true")
        void tokenDelMismoUsuario_retornaTrue() {
            String token = jwtService.generarTokenBasico(userDetails);
            assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
        }

        @Test
        @DisplayName("token de otro usuario retorna false")
        void tokenDeOtroUsuario_retornaFalse() {
            UserDetails otroUsuario = new User(
                    "otro@test.com", "$2a$hashed",
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
            String tokenParaOtro = jwtService.generarTokenBasico(otroUsuario);
            assertThat(jwtService.isTokenValid(tokenParaOtro, userDetails)).isFalse();
        }

        @Test
        @DisplayName("token caducado lanza ExpiredJwtException")
        void tokenCaducado_lanzaExcepcion() {
            // Simula una ventana de expiración en el pasado para forzar la excepción ExpiredJwtException
            ReflectionTestUtils.setField(jwtService, "duracion", -1000L);
            String tokenCaducado = jwtService.generarTokenBasico(userDetails);
            ReflectionTestUtils.setField(jwtService, "duracion", DURACION);

            assertThatThrownBy(() -> jwtService.isTokenValid(tokenCaducado, userDetails))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }
}
