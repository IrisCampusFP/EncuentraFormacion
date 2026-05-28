package com.irisperez.tfg.encuentraformacion.security;

import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.service.auth.CustomUserDetailsService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketAuthInterceptor")
class WebSocketAuthInterceptorTest {

    @Mock private JwtService jwtService;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private ServerHttpResponse response;
    @Mock private WebSocketHandler wsHandler;

    @InjectMocks private WebSocketAuthInterceptor interceptor;

    private Usuario usuario;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(42L);
        usuario.setEmail("iris@test.com");
        usuario.setPassword("hash");
        usuario.setActivo(true);
        usuario.setRoles(List.of());
        userDetails = new CustomUserDetails(usuario);
    }

    @Test
    @DisplayName("cookie válida: acepta handshake y almacena userId")
    void cookieValida_aceptaHandshake() throws Exception {
        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getCookies()).thenReturn(new Cookie[]{ new Cookie("jwt_token", "token-valido") });
        ServletServerHttpRequest request = new ServletServerHttpRequest(httpReq);

        when(jwtService.extraerEmail("token-valido")).thenReturn("iris@test.com");
        when(userDetailsService.loadUserByUsername("iris@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("token-valido", userDetails)).thenReturn(true);

        Map<String, Object> attrs = new HashMap<>();
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attrs);

        assertThat(result).isTrue();
        assertThat(attrs.get("userId")).isEqualTo(42L);
    }

    @Test
    @DisplayName("token inválido: rechaza handshake")
    void tokenInvalido_rechazaHandshake() throws Exception {
        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getCookies()).thenReturn(new Cookie[]{ new Cookie("jwt_token", "token-malo") });
        ServletServerHttpRequest request = new ServletServerHttpRequest(httpReq);

        when(jwtService.extraerEmail("token-malo")).thenReturn("iris@test.com");
        when(userDetailsService.loadUserByUsername("iris@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("token-malo", userDetails)).thenReturn(false);

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("sin cookies: rechaza handshake")
    void sinCookies_rechazaHandshake() throws Exception {
        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getCookies()).thenReturn(null);
        ServletServerHttpRequest request = new ServletServerHttpRequest(httpReq);

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());

        assertThat(result).isFalse();
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("cookie con nombre incorrecto: rechaza handshake")
    void cookieNombreIncorrecto_rechazaHandshake() throws Exception {
        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getCookies()).thenReturn(new Cookie[]{ new Cookie("otro_token", "valor") });
        ServletServerHttpRequest request = new ServletServerHttpRequest(httpReq);

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());

        assertThat(result).isFalse();
        verifyNoInteractions(jwtService);
    }
}
