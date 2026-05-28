package com.irisperez.tfg.encuentraformacion.model.enums;

/**
 * Constantes con los nombres de los roles del sistema.
 * Centraliza los literales "ROLE_*" para evitar errores ortográficos
 * y facilitar futuros cambios en la nomenclatura de roles.
 */
public enum RolNombre {
    ADMIN,
    GESTOR_CENTRO,
    ESTUDIANTE;

    /**
     * Devuelve el nombre del rol con el prefijo ROLE_
     * (necesario para Spring Security)
     */
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
