-- V17: Añadir NUEVA_SOLICITUD al check constraint de notificaciones.tipo
ALTER TABLE notificaciones DROP CONSTRAINT notificaciones_tipo_check;

ALTER TABLE notificaciones
    ADD CONSTRAINT notificaciones_tipo_check
        CHECK (tipo IN ('SOLICITUD_APROBADA', 'SOLICITUD_RECHAZADA', 'NUEVO_MENSAJE', 'NUEVA_SOLICITUD'));
