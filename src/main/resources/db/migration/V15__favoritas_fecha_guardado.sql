ALTER TABLE formaciones_favoritas
    ADD COLUMN fecha_guardado TIMESTAMP NOT NULL DEFAULT NOW();
