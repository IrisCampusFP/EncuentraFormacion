package com.irisperez.tfg.encuentraformacion.controller.estudiante;

import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Rol;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.formacion.FavoritoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoritoController")
class FavoritoControllerTest {

    @Mock private FavoritoService favoritoService;
    @InjectMocks private FavoritoController controller;

    private CustomUserDetails userDetails;
    private UUID uuid;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        Usuario u = new Usuario(); u.setId(1L);
        Rol rol = new Rol(); rol.setNombre(RolNombre.ESTUDIANTE);
        u.setRoles(List.of(rol));
        userDetails = new CustomUserDetails(u);
        uuid = UUID.randomUUID();
        pageable = PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "fechaAlta"));
    }

    @Test
    @DisplayName("getMisFavoritos devuelve página con contenido")
    void getMisFavoritos_ok() {
        FormacionResumenDTO dto = new FormacionResumenDTO(); dto.setId(1L);
        Page<FormacionResumenDTO> page = new PageImpl<>(List.of(dto), pageable, 1);
        when(favoritoService.getMisFavoritos(eq(1L), any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<FormacionResumenDTO>> resp = controller.getMisFavoritos(pageable, userDetails);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getMisFavoritos devuelve página vacía")
    void getMisFavoritos_vacio() {
        Page<FormacionResumenDTO> empty = Page.empty(pageable);
        when(favoritoService.getMisFavoritos(eq(1L), any(Pageable.class))).thenReturn(empty);

        ResponseEntity<Page<FormacionResumenDTO>> resp = controller.getMisFavoritos(pageable, userDetails);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("getEstado devuelve guardada=true")
    void getEstado_guardada() {
        when(favoritoService.esGuardada(uuid, 1L)).thenReturn(true);

        ResponseEntity<Map<String, Boolean>> resp = controller.getEstado(uuid, userDetails);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("guardada", true);
    }

    @Test
    @DisplayName("getEstado devuelve guardada=false")
    void getEstado_noGuardada() {
        when(favoritoService.esGuardada(uuid, 1L)).thenReturn(false);

        ResponseEntity<Map<String, Boolean>> resp = controller.getEstado(uuid, userDetails);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("guardada", false);
    }

    @Test
    @DisplayName("agregar devuelve 204")
    void agregar_ok() {
        doNothing().when(favoritoService).agregar(uuid, 1L);

        ResponseEntity<Void> resp = controller.agregar(uuid, userDetails);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("quitar devuelve 204")
    void quitar_ok() {
        doNothing().when(favoritoService).quitar(uuid, 1L);

        ResponseEntity<Void> resp = controller.quitar(uuid, userDetails);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
