package com.irisperez.tfg.encuentraformacion.service.auth;

import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.repository.UsuarioRepository;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio que se encarga de obtener los datos del usuario para Spring Security.

 * Creamos un UserDetailsService personalizado heredando la interfaz original de Spring Security
 * y sobreescribiendo el metodo loadUserByUsername para obtener los datos del usuario
 * mediante el email (ya que el UserDetailsService original busca por username).
 */
@Service
@RequiredArgsConstructor
@NullMarked // Indica que todos los parametros y retornos de métodos de esta clase no pueden ser null a menos que se indique lo contrario (con @Nullable)
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    // Spring Security llama a este metodo en cada login para cargar el usuario por email
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No existe ningún usuario registrado con el email: " + email));
        return new CustomUserDetails(usuario);
    }
}
