package com.irisperez.tfg.encuentraformacion.controller.gestor;

import com.irisperez.tfg.encuentraformacion.dto.chat.ConversacionGestorDTO;
import com.irisperez.tfg.encuentraformacion.dto.chat.EnviarMensajeDTO;
import com.irisperez.tfg.encuentraformacion.dto.chat.EventoSolicitudChatDTO;
import com.irisperez.tfg.encuentraformacion.dto.chat.MensajeChatDTO;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.chat.ChatGestorService;
import com.irisperez.tfg.encuentraformacion.service.solicitud.formacion.EventoSolicitudChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/gestor/chat")
@RequiredArgsConstructor
public class ChatGestorController {

    private final ChatGestorService service;
    private final EventoSolicitudChatService eventoSolicitudChatService;

    @GetMapping("/count-no-leidos")
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<Long> countNoLeidos(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(service.countMensajesNoLeidos(user.getId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<List<ConversacionGestorDTO>> getConversaciones(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) Long formacionId) {
        return ResponseEntity.ok(service.getConversaciones(user.getId(), formacionId));
    }

    @GetMapping("/{id}/mensajes")
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<List<MensajeChatDTO>> getMensajes(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        return ResponseEntity.ok(service.getMensajes(user.getId(), id));
    }

    @PostMapping("/{id}/mensajes")
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<MensajeChatDTO> enviarMensaje(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody EnviarMensajeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.enviarMensaje(user.getId(), id, dto));
    }

    @GetMapping("/{id}/eventos-solicitud")
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<List<EventoSolicitudChatDTO>> getEventosSolicitud(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        return ResponseEntity.ok(eventoSolicitudChatService.getEventosGestor(id, user.getId()));
    }
}
