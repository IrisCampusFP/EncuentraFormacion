package com.irisperez.tfg.encuentraformacion.security;

import com.irisperez.tfg.encuentraformacion.model.entity.TokenInvalidado;
import com.irisperez.tfg.encuentraformacion.repository.TokenInvalidadoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private TokenInvalidadoRepository repository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private TokenBlacklistService service;

    @Test
    void debeDetectarTokenInvalidado() {
        // given: un token cuyo hash ya está en el repositorio
        String token = "mi.jwt.token";
        when(repository.existsByTokenHash(anyString())).thenReturn(true);
        // when/then
        assertThat(service.isTokenBlacklisted(token)).isTrue();
    }

    @Test
    void debeGuardarHashNoElTokenEnClaro() {
        // given
        when(jwtService.extraerFechaExpiracion(any())).thenReturn(new Date(System.currentTimeMillis() + 86400000));
        when(repository.existsByTokenHash(any())).thenReturn(false);
        // when
        service.blacklistarToken("token.secreto.aqui");
        // then: lo que se guarda NO contiene el texto "token.secreto.aqui"
        ArgumentCaptor<TokenInvalidado> captor = ArgumentCaptor.forClass(TokenInvalidado.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).doesNotContain("token.secreto.aqui");
    }

    @Test
    void debeLimpiarTokensExpirados() {
        when(repository.deleteByFechaExpiracionBefore(any(LocalDateTime.class))).thenReturn(5);
        service.limpiarTokensExpirados();
        verify(repository).deleteByFechaExpiracionBefore(any(LocalDateTime.class));
    }
}
