package com.irisperez.tfg.encuentraformacion.security;

import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Clase que permite acceder fácilmente al ID del usuario y otros campos 
 * desde el contexto de seguridad en cualquier parte de la aplicación.
 */
@Getter
@NullMarked
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> roles;
    private final boolean activo;

    public CustomUserDetails(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.password = usuario.getPassword();
        this.activo = usuario.getActivo() != null && usuario.getActivo();
        this.roles = usuario.getRoles().stream()
                .map(rol -> new SimpleGrantedAuthority(rol.getNombre().getAuthority()))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email; // Usamos el email como identificador único
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
