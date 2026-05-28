# EncuentraFormacion

![Estado](https://img.shields.io/badge/Estado-En_desarrollo-F59E0B?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?style=flat-square&logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-Migraciones-CC0200?style=flat-square&logo=flyway&logoColor=white)
![Bootstrap](https://img.shields.io/badge/Bootstrap-5-7952B3?style=flat-square&logo=bootstrap&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.8+-C71A36?style=flat-square&logo=apachemaven&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)
![Coverage](https://img.shields.io/badge/Cobertura-min_80%25-10B981?style=flat-square&logo=jacoco)

**EncuentraFormacion** es una plataforma que centraliza la búsqueda y gestión de formaciones académicas y profesionales, conectando a estudiantes con centros educativos en un único entorno estructurado.

---

## Requisitos Previos

| Herramienta | Versión mínima | Uso |
|---|---|---|
| Java | 21 | Runtime del backend |
| Docker Desktop | Cualquier versión reciente | Levantar PostgreSQL en local |
| Maven | 3.8+ | Build (o usar el `./mvnw` incluido) |

> PostgreSQL **no** necesita instalarse manualmente. Se levanta vía Docker con un solo comando.

---

## Configuración Local

### 1. Variables de entorno

Copia el archivo de ejemplo y rellena tus valores:

```bash
cp .env.example .env
```

Edita `.env`:

```env
# Configuracion de la base de datos
DB_HOST=localhost
DB_PORT=5433
DB_NAME=nombre_de_tu_base_de_datos
SPRING_DATASOURCE_USERNAME=tu_usuario
SPRING_DATASOURCE_PASSWORD=tu_contrasena

# Cadena aleatoria de al menos 64 caracteres hexadecimales
JWT_SECRET=tu_clave_secreta_aqui

# false en desarrollo local (HTTP), true en produccion (HTTPS)
JWT_COOKIE_SECURE=false

# Asistente IA - Gemini (primario): obtener en https://aistudio.google.com/app/apikey
# Modelo recomendado: gemini-1.5-flash (1500 req/día gratis). Evitar gemini-2.0-flash (solo 200 req/día).
GEMINI_API_KEY=tu_clave_gemini_aqui
GEMINI_MODEL=gemini-2.0-flash-lite

# Asistente IA - Groq (fallback): obtener en https://console.groq.com/keys
GROQ_API_KEY=tu_clave_groq_aqui
```

> El archivo `.env` **nunca** se sube al repositorio (está en `.gitignore`).

### 2. Levantar la base de datos

```bash
docker-compose up -d
```

Esto inicia PostgreSQL 17 en Docker. El schema se crea y migra automáticamente al arrancar la aplicación (gestionado por Flyway).

### 3. Arrancar la aplicación

```bash
./mvnw spring-boot:run
```

Al arrancar, Flyway aplica automáticamente las migraciones pendientes sobre el schema `encuentra_formacion`. La aplicación queda disponible en [http://localhost:8080](http://localhost:8080).

### 4. Cargar datos de ejemplo (opcional)

Para poblar la base de datos con usuarios, centros y formaciones de prueba:

```bash
psql -h localhost -U postgres -d encuentra_formacion_desarrollo \
     -f src/main/resources/db/sample/datos_ejemplo.sql
```

---

## Comandos de desarrollo

| Acción | Comando |
|---|---|
| Iniciar BD (Docker) | `docker-compose up -d` |
| Parar BD | `docker-compose down` |
| Arrancar app | `./mvnw spring-boot:run` |
| Ejecutar todos los tests | `./mvnw test` |
| Tests + informe de cobertura | `./mvnw verify` |
| Test de una clase | `./mvnw test -Dtest=NombreTest` |
| Build (genera .jar) | `./mvnw clean package` |

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| **Backend** | Spring Boot 4.0.6, Spring Security, Spring Data JPA |
| **Seguridad** | JWT en Cookie HttpOnly, Bucket4j (Rate Limiting), BCrypt |
| **ORM / Mappers** | Hibernate (modo `validate`), MapStruct, Lombok |
| **Migraciones de BD** | Flyway — versionado automático del schema |
| **Base de Datos** | PostgreSQL 17 (via Docker) |
| **Frontend** | HTML5, CSS3 (Bootstrap 5), JavaScript vanilla |
| **Asistente IA** | Gemini 2.0 Flash Lite (primario) + Groq llama-3.3-70b (fallback), function calling |
| **Build / Calidad** | Maven, JaCoCo (cobertura ≥80%) |

---

## Arquitectura de seguridad

- **Autenticación**: JWT almacenado en Cookie `HttpOnly` (inaccesible desde JS → protección XSS).
- **Rate Limiting**: 5 intentos de login por IP por minuto con Bucket4j (protección fuerza bruta).
- **Autorización**: `@PreAuthorize` por método + `SecurityFilterChain` con rutas por rol.
- **Contraseñas**: BCrypt (bean centralizado en `SecurityConfig`).
- **Schema bloqueado**: Hibernate en modo `validate` — solo Flyway modifica el schema.

---

## Autora

**Iris Pérez Aparicio** — [IrisCampusFP](https://github.com/IrisCampusFP)
