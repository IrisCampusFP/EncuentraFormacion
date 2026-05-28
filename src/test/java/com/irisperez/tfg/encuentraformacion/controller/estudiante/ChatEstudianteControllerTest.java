package com.irisperez.tfg.encuentraformacion.controller.estudiante;

import com.irisperez.tfg.encuentraformacion.dto.chat.*;
import com.irisperez.tfg.encuentraformacion.model.entity.Rol;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.chat.ChatEstudianteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatEstudianteControllerTest {

    @Mock
    private ChatEstudianteService chatEstudianteService;

    @InjectMocks
    private ChatEstudianteController controller;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        Usuario u = new Usuario();
        u.setId(1L);
        Rol rol = new Rol();
        rol.setNombre(RolNombre.ESTUDIANTE);
        u.setRoles(java.util.List.of(rol));
        userDetails = new CustomUserDetails(u);
    }

    @Test
    void iniciar() {
        IniciarChatDTO dto = new IniciarChatDTO();
        ConversacionResumenDTO resp = new ConversacionResumenDTO();
        when(chatEstudianteService.iniciarORecuperar(dto, 1L)).thenReturn(resp);

        ResponseEntity<ConversacionResumenDTO> response = controller.iniciar(dto, userDetails);

        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    void getMisConversaciones() {
        when(chatEstudianteService.getMisConversaciones(1L)).thenReturn(List.of());

        ResponseEntity<List<ConversacionResumenDTO>> response = controller.getMisConversaciones(userDetails);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void enviarMensaje() {
        EnviarMensajeDTO dto = new EnviarMensajeDTO();
        MensajeChatDTO resp = new MensajeChatDTO();
        when(chatEstudianteService.enviarMensaje(2L, dto, 1L)).thenReturn(resp);

        ResponseEntity<MensajeChatDTO> response = controller.enviarMensaje(2L, dto, userDetails);

        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    void getMensajes() {
        ConversacionMensajesDTO dto = new ConversacionMensajesDTO();
        when(chatEstudianteService.getMensajes(2L, 1L)).thenReturn(dto);

        ResponseEntity<ConversacionMensajesDTO> response = controller.getMensajes(2L, userDetails);

        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void desvincularFormacion() {
        UUID uuid = UUID.randomUUID();
        ResponseEntity<Void> response = controller.desvincularFormacion(2L, uuid, userDetails);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void desvincularFormacion_requiresEstudianteRole() throws NoSuchMethodException {
        java.lang.reflect.Method method = ChatEstudianteController.class.getMethod(
                "desvincularFormacion", Long.class, UUID.class, CustomUserDetails.class);
        org.springframework.security.access.prepost.PreAuthorize preAuthorize = 
                method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('ESTUDIANTE')");
    }
}
