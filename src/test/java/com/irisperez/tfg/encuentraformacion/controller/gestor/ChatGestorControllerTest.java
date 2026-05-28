package com.irisperez.tfg.encuentraformacion.controller.gestor;

import com.irisperez.tfg.encuentraformacion.dto.chat.ConversacionGestorDTO;
import com.irisperez.tfg.encuentraformacion.dto.chat.EnviarMensajeDTO;
import com.irisperez.tfg.encuentraformacion.dto.chat.EventoSolicitudChatDTO;
import com.irisperez.tfg.encuentraformacion.dto.chat.MensajeChatDTO;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.chat.ChatGestorService;
import com.irisperez.tfg.encuentraformacion.service.solicitud.formacion.EventoSolicitudChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatGestorControllerTest {

    @Mock private ChatGestorService service;
    @Mock private EventoSolicitudChatService eventoSolicitudChatService;
    @InjectMocks private ChatGestorController controller;

    private CustomUserDetails user(long id) {
        CustomUserDetails u = mock(CustomUserDetails.class);
        when(u.getId()).thenReturn(id);
        return u;
    }

    @Test
    void getConversaciones_ok() {
        when(service.getConversaciones(1L, null)).thenReturn(List.of(new ConversacionGestorDTO()));

        var resp = controller.getConversaciones(user(1L), null);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    void getMensajes_ok() {
        when(service.getMensajes(1L, 5L)).thenReturn(List.of(new MensajeChatDTO()));

        var resp = controller.getMensajes(user(1L), 5L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    void enviarMensaje_devuelve201() {
        var dto = new EnviarMensajeDTO();
        var resultado = new MensajeChatDTO();
        when(service.enviarMensaje(1L, 5L, dto)).thenReturn(resultado);

        var resp = controller.enviarMensaje(user(1L), 5L, dto);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isSameAs(resultado);
    }

    @Test
    void getEventosSolicitud_ok() {
        when(eventoSolicitudChatService.getEventosGestor(5L, 1L)).thenReturn(List.of(new EventoSolicitudChatDTO()));

        var resp = controller.getEventosSolicitud(user(1L), 5L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
    }
}
