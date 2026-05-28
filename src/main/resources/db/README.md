# Base de datos — Backup y restauración

## Contenido

| Fichero | Descripción |
|---|---|
| `backup_ef.dump` | Backup completo del schema `encuentra_formacion` con datos reales del Ministerio de Educación (121.635 formaciones). Generado el 2026-05-26. |
| `migration/` | Scripts Flyway que gestionan el esquema (DDL). |
| `sample/` | Datos de muestra para desarrollo. |

---

## Exportar (crear backup)

```bash
# 1. Ejecutar pg_dump dentro del contenedor (el fichero queda en el sistema de ficheros del contenedor)
docker exec encuentra_formacion_db bash -c \
  "pg_dump -U postgres -d encuentra_formacion --schema=encuentra_formacion -F c > /tmp/backup_ef.dump"

# 2. Copiar el fichero del contenedor a tu máquina local
docker cp encuentra_formacion_db:/tmp/backup_ef.dump \
  src/main/resources/db/backup_ef.dump
```

---

## Importar (restaurar backup)

> Requisitos: el contenedor Docker debe estar levantado (`docker-compose up -d`) y Flyway debe haber ejecutado las migraciones al menos una vez para que el schema exista.

```bash
# 1. Copiar el fichero de tu máquina local al contenedor
docker cp src/main/resources/db/backup_ef.dump \
  encuentra_formacion_db:/tmp/backup_ef.dump

# 2. Ejecutar la restauración dentro del contenedor
docker exec encuentra_formacion_db pg_restore \
  -U postgres -d encuentra_formacion \
  --schema=encuentra_formacion \
  -F c --no-owner --no-privileges \
  /tmp/backup_ef.dump
```

Si hay conflictos por datos ya existentes, añade `--clean` antes de `--no-owner` para borrar y reinsertar.

---

## Verificar tras restaurar

```bash
docker exec -it encuentra_formacion_db psql -U postgres -d encuentra_formacion \
  -c "SET search_path TO encuentra_formacion; SELECT COUNT(*) FROM formaciones;"
# Debe devolver ~121635
```
