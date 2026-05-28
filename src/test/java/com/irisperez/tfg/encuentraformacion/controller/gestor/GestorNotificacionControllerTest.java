package com.irisperez.tfg.encuentraformacion.controller.gestor;

import com.irisperez.tfg.encuentraformacion.dto.notificacion.NotificacionDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Rol;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.notificacion.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestorNotificacionControllerTest {

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private GestorNotificacionController controller;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        Usuario u = new Usuario();
        u.setId(2L);
        Rol rol = new Rol();
        rol.setNombre(RolNombre.GESTOR_CENTRO);
        u.setRoles(List.of(rol));
        userDetails = new CustomUserDetails(u);
    }

    @Test
    void getMisNotificaciones() {
        Pageable pageable = PageRequest.of(0, 10);
        when(notificacionService.getMisNotificaciones(eq(2L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<Page<NotificacionDTO>> response = controller.getMisNotificaciones(pageable, userDetails);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void countNoLeidas() {
        when(notificacionService.countNoLeidas(2L)).thenReturn(3L);

        ResponseEntity<Map<String, Long>> response = controller.countNoLeidas(userDetails);

        assertThat(response.getBody()).containsEntry("count", 3L);
    }

    @Test
    void marcarLeida() {
        ResponseEntity<Void> response = controller.marcarLeida(10L, userDetails);

        verify(notificacionService).marcarLeida(10L, 2L);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void marcarTodasLeidas() {
        ResponseEntity<Void> response = controller.marcarTodasLeidas(userDetails);

        verify(notificacionService).marcarTodasLeidas(2L);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void marcarLeidasPorConversacion() {
        ResponseEntity<Void> response = controller.marcarLeidasPorConversacion(5L, userDetails);

        verify(notificacionService).marcarLeidasPorConversacion(2L, 5L);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
