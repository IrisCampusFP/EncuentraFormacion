package com.irisperez.tfg.encuentraformacion.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private SecurityAuditService auditService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("sin token")
    class SinToken {

        @Test
        @DisplayName("sin encabezado ni cookie pasa al siguiente filtro sin autenticar")
        void sinTokenEnNingunLado_pasaAlSiguienteFiltro() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);
            when(request.getCookies()).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("encabezado sin prefijo Bearer pasa al siguiente filtro sin autenticar")
        void encabezadoSinBearer_pasaSinAutenticar() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");
            when(request.getCookies()).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("token en encabezado Authorization")
    class TokenEnHeader {

        @Test
        @DisplayName("token válido en header autentica al usuario en el contexto de seguridad")
        void tokenValidoEnHeader_autenticaUsuario() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer token.valido.aqui");
            when(tokenBlacklistService.isTokenBlacklisted("token.valido.aqui")).thenReturn(false);
            when(jwtService.extraerEmail("token.valido.aqui")).thenReturn("usuario@test.com");
            when(userDetailsService.loadUserByUsername("usuario@test.com")).thenReturn(userDetails);
            when(jwtService.isTokenValid("token.valido.aqui", userDetails)).thenReturn(true);
            when(userDetails.getAuthorities()).thenReturn(List.of());

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        }
    }

    @Nested
    @DisplayName("token en cookie")
    class TokenEnCookie {

        @Test
        @DisplayName("token válido en cookie jwt_token autentica al usuario")
        void tokenValidoEnCookie_autenticaUsuario() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);
            Cookie jwtCookie = new Cookie("jwt_token", "token.desde.cookie");
            when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});
            when(tokenBlacklistService.isTokenBlacklisted("token.desde.cookie")).thenReturn(false);
            when(jwtService.extraerEmail("token.desde.cookie")).thenReturn("user@test.com");
            when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
            when(jwtService.isTokenValid("token.desde.cookie", userDetails)).thenReturn(true);
            when(userDetails.getAuthorities()).thenReturn(List.of());

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        }

        @Test
        @DisplayName("cookie con nombre distinto a jwt_token no extrae el token")
        void cookieConNombreDistinto_noBuscaToken() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);
            Cookie otraCookie = new Cookie("session_id", "abc123");
            when(request.getCookies()).thenReturn(new Cookie[]{otraCookie});

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService);
        }
    }

    @Nested
    @DisplayName("token en lista negra")
    class TokenEnListaNegra {

        @Test
        @DisplayName("token revocado pasa al siguiente filtro sin autenticar y registra auditoría")
        void tokenRevocado_pasaSinAutenticar() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer token.revocado");
            when(tokenBlacklistService.isTokenBlacklisted("token.revocado")).thenReturn(true);
            when(request.getRequestURI()).thenReturn("/api/recurso");
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(auditService).tokenInvalidado("127.0.0.1");
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verifyNoMoreInteractions(jwtService);
        }
    }

    @Nested
    @DisplayName("excepciones de token")
    class ExcepcionesDeToken {

        @Test
        @DisplayName("token expirado es manejado y el filtro continúa sin autenticar")
        void tokenExpirado_manejaExcepcionYContinua() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer token.expirado");
            when(tokenBlacklistService.isTokenBlacklisted("token.expirado")).thenReturn(false);
            when(jwtService.extraerEmail("token.expirado"))
                    .thenThrow(new ExpiredJwtException(null, null, "Token expirado"));
            when(request.getRequestURI()).thenReturn("/api/recurso");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("token malformado lanza JwtException y el filtro continúa sin autenticar")
        void tokenMalformado_manejaExcepcionYContinua() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer token.invalido");
            when(tokenBlacklistService.isTokenBlacklisted("token.invalido")).thenReturn(false);
            when(jwtService.extraerEmail("token.invalido"))
                    .thenThrow(new JwtException("Token inválido"));
            when(request.getRequestURI()).thenReturn("/api/recurso");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("excepción genérica inesperada no interrumpe la cadena de filtros")
        void excepcionGenerica_manejaYContinua() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer token.error");
            when(tokenBlacklistService.isTokenBlacklisted("token.error")).thenReturn(false);
            when(jwtService.extraerEmail("token.error"))
                    .thenThrow(new RuntimeException("Error inesperado"));
            when(request.getRequestURI()).thenReturn("/api/recurso");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("token válido pero no válido para el userDetails no autentica")
        void tokenValidoSintacticamentePeroInvalido_noAutentica() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer token.ok");
            when(tokenBlacklistService.isTokenBlacklisted("token.ok")).thenReturn(false);
            when(jwtService.extraerEmail("token.ok")).thenReturn("user@test.com");
            when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
            when(jwtService.isTokenValid("token.ok", userDetails)).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }
}
