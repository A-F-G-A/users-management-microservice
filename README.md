# Users Management — Spring Boot, Arquitectura Hexagonal y DDD

Aplicación de gestión de usuarios construida con Java 17 y Spring Boot. La API REST es el punto de entrada activo. El código de la antigua CLI se conserva como adaptador inactivo y no posee un contenedor de dependencias independiente.

Spring es el único *composition root*: `Main` inicia el contexto y las dependencias se resuelven mediante configuración y component scanning de Spring.

## Verificación

`GET /health` es publico y responde HTTP 200 con los metadatos generados desde
`pom.xml` durante el build:

```json
{"status":"UP","name":"users-management","version":"2.2.0"}
```

Indica que la API responde; no comprueba MySQL ni el proveedor de correo.

```bash
bash ./mvnw clean test
bash ./mvnw clean package
```

En Windows se puede utilizar `mvnw.cmd`.

## CRUDL de celulares (ejercicio 11)

La API conserva Spring JDBC y separa el lado de escritura (`CellphoneCommandHandler` y
`CellphoneWritePort`) del lado de lectura (`CellphoneQueryHandler` y
`CellphoneReadPort`). El dominio no depende de Spring ni de SQL. El IMEI se representa
como texto de 15 dígitos, mantiene ceros iniciales y tiene una restricción única en MySQL.
Los campos booleanos son obligatorios, por lo que `false` se conserva como valor real;
el almacenamiento secundario puede ser nulo cuando el equipo no lo ofrece.

| Método | Ruta | Permiso |
|---|---|---|
| POST | `/api/users` | Público (registro existente) |
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
`huella`, `ir`, `resisteAgua` y `cantidadSim`. Un IMEI duplicado devuelve 409, un
recurso inexistente 404 y una entrada inválida 400.

Las migraciones Flyway se ejecutan al iniciar la aplicación. Una base ya existente se
marca en la versión 1 sin alterar sus datos; la migración 2 añade únicamente la tabla
`cellphones`. La migración inicial sólo crea la tabla `users` si falta, para instalaciones
nuevas.

En una base nueva que necesite el usuario inicial de administración, ejecute
`src/main/resources/schema.sql` una sola vez antes del despliegue; la inicialización del
contenedor local ya lo hace. No ejecute ese script sobre una base con datos existentes.
Flyway baselina el esquema previo y agrega Celular sin borrar o reemplazar datos.

### Ejecutar con Docker Compose

Copie `.env.example` a `.env`, defina contraseñas locales y configure `JWT_SECRET` como
Base64 de al menos 32 bytes aleatorios. Por ejemplo, en PowerShell:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Después ejecute `docker compose up --build`. El secreto JWT y las contraseñas son
obligatorios; no hay valores de desarrollo predeterminados en la configuración activa.

### Postman

Importe `postman/Users-Management-Exercise-11.postman_collection.json` y
`postman/Users-Management-Local.postman_environment.json`. Defina un token JWT con rol
ADMIN para operaciones protegidas y complete las variables de usuario. La petición
Login guarda el token de la respuesta automáticamente.

### CI y Render

GitHub Actions ejecuta `bash ./mvnw clean verify` y construye la imagen Docker en push o pull
request. `render.yaml` conserva la rama `main` para evitar que esta rama de actividad
publique automáticamente en producción. El servicio existente debe recibir la rama
mediante el flujo aprobado por el propietario; no se realizó un despliegue desde aquí.

La API documenta los permisos mediante OpenAPI; Swagger UI está en `/swagger-ui.html`.
La explicación académica y el guion de sustentación están en `docs/`.

## Despliegue en Render

El archivo `render.yaml` define el servicio web, el build con Docker y el despliegue
automático de cada commit que llegue a la rama `main`.

1. Crear un Blueprint en Render y seleccionar este repositorio.
2. Completar en el panel los secretos marcados como requeridos: `DB_HOST`,
   `DB_USERNAME`, `DB_PASSWORD`, `SMTP_USERNAME`, `SMTP_PASSWORD` y
   `SMTP_FROM_ADDRESS`.
3. Usar una instancia MySQL accesible desde Internet o desde la red privada de
   Render y ejecutar `src/main/resources/schema.sql` una vez para crear el esquema.
4. Desplegar el Blueprint. La API quedará disponible en el subdominio
   `onrender.com` asignado por Render y Swagger UI en `/swagger-ui.html`.

Las credenciales nunca deben guardarse en `application.properties` ni en
`render.yaml`. Para desarrollo local, deben proporcionarse como variables de entorno.
