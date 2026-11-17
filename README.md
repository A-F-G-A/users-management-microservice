# Users Management — Spring Boot, Arquitectura Hexagonal y DDD

Aplicación de gestión de usuarios construida con Java 17 y Spring Boot. La API REST es el punto de entrada activo. El código de la antigua CLI se conserva como adaptador inactivo y no posee un contenedor de dependencias independiente.

Spring es el único *composition root*: `Main` inicia el contexto y las dependencias se resuelven mediante configuración y component scanning de Spring.

## Verificación

`GET /health` es público y responde HTTP 200 con los metadatos generados desde
`pom.xml` durante el build:

```json
{"status":"UP","name":"users-management","version":"2.2.0"}
```

Indica que la API responde; no comprueba MySQL ni el proveedor de correo.

```bash
bash ./mvnw clean verify   # pruebas unitarias + integración MySQL + JAR
```

En Windows se puede utilizar `mvnw.cmd clean verify`.

`verify` ejecuta dos suites:

- **Unitarias** (Surefire, `*Test`): dominio, casos de uso, mappers, seguridad y MockMvc.
- **Integración** (Failsafe, `*IT`): MySQL 8.4 real con Flyway habilitado, sin mocks de
  `JdbcTemplate` ni `ResultSet`. Usan Testcontainers si Docker está disponible. Sin Docker
  se pueden dirigir a un servidor MySQL **desechable** con `IT_MYSQL_HOST`, `IT_MYSQL_PORT`,
  `IT_MYSQL_USER` e `IT_MYSQL_PASSWORD` (usuario con permiso `CREATE DATABASE`; cada clase
  crea su propia base `it_*`). Si no hay ninguno, se informan como *omitidas*. En GitHub
  Actions `IT_REQUIRE_MYSQL=true` hace que la ausencia de MySQL **falle** el build.

## CRUDL de celulares (ejercicio 11)

La API conserva Spring JDBC y separa el lado de escritura (`CellphoneCommandHandler` y
`CellphoneWritePort`) del lado de lectura (`CellphoneQueryHandler`, `@Transactional(readOnly = true)`,
y `CellphoneReadPort`). El dominio no depende de Spring ni de SQL. El IMEI se representa
como texto de 15 dígitos, conserva ceros iniciales, se valida con el dígito verificador
Luhn y tiene una restricción única en MySQL. Los campos booleanos son obligatorios, por
lo que `false` se conserva como valor real; el almacenamiento secundario puede ser nulo.
`CellphoneModel` valida también longitudes (texto ≤ 100, id ≤ 36) y límites numéricos
iguales a las columnas `DECIMAL(6,2)` y `DECIMAL(7,2)`, de modo que ningún adaptador puede
persistir un celular inválido.

| Método | Ruta | Permiso |
|---|---|---|
| POST | `/api/users` | Público (autorregistro: siempre `MEMBER`, estado `PENDING`) |
| GET | `/api/users`, `/api/users/{id}` | ADMIN o REVIEWER |
| PUT, DELETE | `/api/users/{id}` | ADMIN |
| POST | `/api/cellphones` | ADMIN |
| GET | `/api/cellphones`, `/api/cellphones/{id}` | ADMIN o REVIEWER |
| PUT, DELETE | `/api/cellphones/{id}` | ADMIN |
| POST | `/api/auth/login` | Público |
| GET | `/health` | Público |

Celulares usan UUID generado por el servidor y las propiedades del cuerpo HTTP siguen
los nombres académicos en español: `marca`, `imei`, `pulgadas`, `megapixeles`, `ram`,
`almacenamientoPrincipal`, `almacenamientoSecundario`, `sistemaOperativo`, `operador`,
`tecnologiaBanda`, `wifi`, `bluetooth`, `camaras`, `marcaCpu`, `velocidadCpu`, `nfc`,
`huella`, `ir`, `resisteAgua` y `cantidadSim`.

| Código | Cuándo |
|---|---|
| 400 | JSON mal formado, tipos inválidos, validación REST o regla de dominio (p. ej. Luhn) |
| 401 | Sin token, token inválido o usuario ya no activo |
| 403 | Rol insuficiente |
| 404 | Recurso inexistente |
| 409 | IMEI o correo duplicado (otros conflictos de unicidad: mensaje genérico) |
| 500 | Solo errores internos; se registran en el log y la respuesta no expone detalles |

## Seguridad

- **Registro público**: `POST /api/users` no acepta `id` ni `role`; si el cliente los envía
  se ignoran. El servidor genera el UUID y asigna `MEMBER` con estado `PENDING`. Solo un
  ADMIN puede activar o cambiar el rol mediante `PUT /api/users/{id}`.
- **Correo de bienvenida**: nunca incluye contraseñas. El usuario define su propia
  contraseña al registrarse (se guarda con BCrypt) y el correo solo informa el estado.
- **JWT**: `JWT_SECRET` es obligatorio (Base64, ≥ 32 bytes), sin valores predeterminados.
  En cada petición el filtro vuelve a leer el usuario: si fue desactivado, bloqueado o
  eliminado, el token deja de autenticar (401); si cambió su rol, se aplica el rol actual.
  Costo: una consulta por clave primaria por petición autenticada.

### Primer administrador (instalaciones nuevas)

Ningún script SQL contiene usuarios, contraseñas ni hashes. Para crear el primer ADMIN:

1. Defina en el entorno `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD` (mínimo 12
   caracteres, única y no reutilizada) y opcionalmente `BOOTSTRAP_ADMIN_NAME`.
2. Inicie la aplicación. Si **no existe ningún ADMIN**, se crea uno `ACTIVE` con la
   contraseña en BCrypt. Si ya existe un ADMIN, las variables se ignoran (se registra una
   advertencia), por lo que no permiten crear administradores adicionales.
3. Elimine `BOOTSTRAP_ADMIN_PASSWORD` del entorno y reinicie.

Los administradores adicionales se crean promoviendo un usuario registrado con
`PUT /api/users/{id}` desde una cuenta ADMIN.

## Migraciones y datos existentes

Las migraciones Flyway se ejecutan al iniciar la aplicación. Una base ya existente se
marca en la versión 1 sin alterar sus datos; la migración 2 añade únicamente la tabla
`cellphones`. La migración inicial sólo crea la tabla `users` si falta. Ambos casos (base
nueva y esquema previo sin Flyway) se comprueban en `FlywayMigrationIT` contra MySQL 8.4.
`src/main/resources/schema.sql` queda como referencia del esquema heredado y no inserta datos.

## Ejecutar con Docker Compose

Copie `.env.example` a `.env`, defina contraseñas locales y configure `JWT_SECRET` como
Base64 de al menos 32 bytes aleatorios. Por ejemplo, en PowerShell:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Para la primera ejecución defina también `BOOTSTRAP_ADMIN_EMAIL` y
`BOOTSTRAP_ADMIN_PASSWORD`. Después ejecute `docker compose up --build`.

- MySQL **no** se publica en el anfitrión; solo la API lo alcanza por la red interna.
- La API se publica en `127.0.0.1:8080` (solo loopback).
- El healthcheck de MySQL ejecuta `SELECT 1` autenticado por TCP con la cuenta de la
  aplicación; la API arranca cuando la base está realmente disponible.
- Flyway crea el esquema; no se monta ningún script SQL de inicialización.

## Postman

Importe `postman/Users-Management-Exercise-11.postman_collection.json` y
`postman/Users-Management-Local.postman_environment.json`. Complete `adminEmail` y
`adminPassword` (el ADMIN creado con `BOOTSTRAP_ADMIN_*`) y `userPassword`. La petición
Login guarda el token automáticamente. La colección incluye los casos 201/200/204, 400
(IMEI de longitud inválida y dígito Luhn incorrecto), 401, 404, 409 y un intento de
autorregistro con `role=ADMIN` que debe quedar como `MEMBER`.

## CI

GitHub Actions (`.github/workflows/ci.yml`) usa `permissions: contents: read`, acciones
fijadas por SHA de commit y `persist-credentials: false`. Ejecuta `bash ./mvnw clean verify`
(incluidas las pruebas MySQL con Testcontainers), valida `compose.yaml` y construye la
imagen Docker.

La API documenta los permisos mediante OpenAPI (versión tomada de `build-info`); Swagger UI
está en `/swagger-ui.html`. La explicación académica y el guion de sustentación están en `docs/`.

## Despliegue en Render

El archivo `render.yaml` define el servicio web, el build con Docker y el despliegue
automático de cada commit que llegue a la rama `main`; esta rama de actividad no publica.

1. Crear un Blueprint en Render y seleccionar este repositorio.
2. Completar en el panel los secretos marcados como requeridos: `DB_HOST`, `DB_PORT`,
   `DB_USERNAME`, `DB_PASSWORD`, `SMTP_USERNAME`, `SMTP_PASSWORD` y `SMTP_FROM_ADDRESS`.
   `JWT_SECRET` lo genera Render.
3. Usar una instancia MySQL accesible desde Render. Flyway crea o actualiza el esquema al
   arrancar; no es necesario ejecutar scripts manuales.
4. Solo en una base sin ADMIN: definir temporalmente `BOOTSTRAP_ADMIN_EMAIL` y
   `BOOTSTRAP_ADMIN_PASSWORD`, desplegar y luego eliminar la contraseña del panel.

**Bases creadas con versiones anteriores**: la versión previa de `schema.sql` insertaba
`admin@example.com` con un hash publicado en el repositorio. Si esa fila existe en una
base real, cambie su contraseña o desactívela; la aplicación no la modifica automáticamente.

Las credenciales nunca deben guardarse en `application.properties` ni en
`render.yaml`. Para desarrollo local, deben proporcionarse como variables de entorno.
