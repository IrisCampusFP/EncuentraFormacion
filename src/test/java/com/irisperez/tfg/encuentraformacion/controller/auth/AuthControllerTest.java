package com.irisperez.tfg.encuentraformacion.controller.auth;

import com.irisperez.tfg.encuentraformacion.dto.auth.AuthResponseDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.LoginRequestDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Rol;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.security.JwtService;
import com.irisperez.tfg.encuentraformacion.security.SecurityAuditService;
import com.irisperez.tfg.encuentraformacion.security.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private SecurityAuditService auditService;
    @Mock private HttpServletRequest httpServletRequest;
    @Mock private HttpServletResponse httpServletResponse;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authController, "jwtExpiration", 3600000L);
        ReflectionTestUtils.setField(authController, "cookieSecure", false);
    }

    private CustomUserDetails buildPrincipal() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("admin@test.com");
        usuario.setPassword("$2a$hashed");
        usuario.setActivo(true);
        Rol rol = new Rol();
        rol.setNombre(RolNombre.ADMIN);
        rol.setUsuarios(new HashSet<>());
        usuario.setRoles(new ArrayList<>(List.of(rol)));
        usuario.setCentrosGestionados(new HashSet<>());
        return new CustomUserDetails(usuario);
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("credenciales válidas retorna 200 con email y roles en cuerpo")
        void credencialesValidas_retorna200ConBodyCorrecto() {
            CustomUserDetails principal = buildPrincipal();
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());

            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtService.generarTokenBasico(principal)).thenReturn("jwt.token.falso");

            LoginRequestDTO request = LoginRequestDTO.builder()
                    .email("admin@test.com")
                    .password("Password1")
                    .build();

            ResponseEntity<AuthResponseDTO> response = authController.login(request, httpServletRequest, httpServletResponse);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getEmail()).isEqualTo("admin@test.com");
            assertThat(response.getBody().getRoles()).contains("ADMIN");
        }

        @Test
        @DisplayName("credenciales incorrectas propaga BadCredentialsException")
        void credencialesIncorrectas_propagaBadCredentialsException() {
            // Fuerza la excepción de malas credenciales en Mockito y valida que responda 401
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

            LoginRequestDTO request = LoginRequestDTO.builder()
                    .email("admin@test.com")
                    .password("wrongpass")
                    .build();

            assertThatThrownBy(() -> authController.login(request, httpServletRequest, httpServletResponse))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Credenciales incorrectas");
        }

        @Test
        @DisplayName("login exitoso establece la cookie de sesión")
        void loginExitoso_estableceCookie() {
            CustomUserDetails principal = buildPrincipal();
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());

            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtService.generarTokenBasico(principal)).thenReturn("jwt.token.falso");

            LoginRequestDTO request = LoginRequestDTO.builder()
                    .email("admin@test.com")
                    .password("Password1")
                    .build();

            authController.login(request, httpServletRequest, httpServletResponse);

            verify(httpServletResponse).addHeader(eq("Set-Cookie"), contains("jwt_token="));
        }

        @Test
        @DisplayName("login exitoso solicita el token al JwtService")
        void loginExitoso_solicitaTokenAlJwtService() {
            CustomUserDetails principal = buildPrincipal();
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());

            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtService.generarTokenBasico(principal)).thenReturn("jwt.token.falso");

            LoginRequestDTO request = LoginRequestDTO.builder()
                    .email("admin@test.com")
                    .password("Password1")
                    .build();

            authController.login(request, httpServletRequest, httpServletResponse);

            verify(jwtService).generarTokenBasico(principal);
        }
    }

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("logout retorna 200 OK")
        void logout_retorna200() {
            ResponseEntity<Void> response = authController.logout(httpServletRequest, httpServletResponse);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("logout establece cookie con maxAge=0 para invalidar la sesión")
        void logout_setCookieConMaxAge0() {
            authController.logout(httpServletRequest, httpServletResponse);
            verify(httpServletResponse).addHeader(eq("Set-Cookie"), contains("Max-Age=0"));
        }

        @Test
        @DisplayName("logout incluye jwt_token en el header de la cookie")
        void logout_cookieJwtTokenPresente() {
            authController.logout(httpServletRequest, httpServletResponse);
            verify(httpServletResponse).addHeader(eq("Set-Cookie"), contains("jwt_token="));
        }

        @Test
        @DisplayName("logout con token en encabezado Authorization lo añade a la lista negra")
        void logout_tokenEnHeader_blacklistaToken() {
            when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer token.activo");
            when(jwtService.extraerEmail("token.activo")).thenReturn("user@test.com");

            authController.logout(httpServletRequest, httpServletResponse);

            verify(tokenBlacklistService).blacklistarToken("token.activo");
            verify(auditService).logout(eq("user@test.com"), any());
        }

        @Test
        @DisplayName("logout con token en cookie lo añade a la lista negra")
        void logout_tokenEnCookie_blacklistaToken() {
            when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwt_token", "token.cookie");
            when(httpServletRequest.getCookies()).thenReturn(new jakarta.servlet.http.Cookie[]{cookie});
            when(jwtService.extraerEmail("token.cookie")).thenReturn("gestor@test.com");

            authController.logout(httpServletRequest, httpServletResponse);

            verify(tokenBlacklistService).blacklistarToken("token.cookie");
            verify(auditService).logout(eq("gestor@test.com"), any());
        }

        @Test
        @DisplayName("logout sin token no invoca blacklistarToken y registra email desconocido")
        void logout_sinToken_noBlacklistaYRegistraDesconocido() {
            when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
            when(httpServletRequest.getCookies()).thenReturn(null);

            authController.logout(httpServletRequest, httpServletResponse);

            verify(tokenBlacklistService, never()).blacklistarToken(anyString());
            verify(auditService).logout(eq("desconocido"), any());
        }
    }
}
