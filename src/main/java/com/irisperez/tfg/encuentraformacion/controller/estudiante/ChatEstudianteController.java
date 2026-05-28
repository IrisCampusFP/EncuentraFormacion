package com.irisperez.tfg.encuentraformacion.controller.estudiante;

import com.irisperez.tfg.encuentraformacion.dto.chat.*;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.chat.ChatEstudianteService;
import com.irisperez.tfg.encuentraformacion.service.solicitud.formacion.EventoSolicitudChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatEstudianteController {

    private final ChatEstudianteService chatEstudianteService;
    private final EventoSolicitudChatService eventoSolicitudChatService;

    @PostMapping("/iniciar")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<ConversacionResumenDTO> iniciar(
            @Valid @RequestBody IniciarChatDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chatEstudianteService.iniciarORecuperar(dto, userDetails.getId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<List<ConversacionResumenDTO>> getMisConversaciones(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chatEstudianteService.getMisConversaciones(userDetails.getId()));
    }

    @PostMapping("/{conversacionId}/mensajes")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<MensajeChatDTO> enviarMensaje(
            @PathVariable Long conversacionId,
            @Valid @RequestBody EnviarMensajeDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chatEstudianteService.enviarMensaje(conversacionId, dto, userDetails.getId()));
    }

    @GetMapping("/{conversacionId}/mensajes")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<ConversacionMensajesDTO> getMensajes(
            @PathVariable Long conversacionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chatEstudianteService.getMensajes(conversacionId, userDetails.getId()));
    }

    @DeleteMapping("/{conversacionId}/formaciones/{formacionUuid}")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Void> desvincularFormacion(
            @PathVariable Long conversacionId,
            @PathVariable UUID formacionUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        chatEstudianteService.desvincularFormacion(conversacionId, formacionUuid, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{conversacionId}/eventos-solicitud")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<List<EventoSolicitudChatDTO>> getEventosSolicitud(
            @PathVariable Long conversacionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
            eventoSolicitudChatService.getEventosEstudiante(conversacionId, userDetails.getId()));
    }
}
