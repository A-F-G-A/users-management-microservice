# Actividad 11: Gestión de celulares

## Objetivo

Extender el microservicio de usuarios existente con operaciones CRUDL para celulares,
conservando Java 17, Spring Boot 3.3.5, MySQL y Spring JDBC. La solución aplica DDD,
Arquitectura Hexagonal, CQRS, SOLID e inversión de dependencias, y corrige los hallazgos
de la auditoría de seguridad, persistencia y API.

## Modelo de dominio

`CellphoneModel` es un `record` inmutable con 21 atributos. Todas las reglas se validan
en el constructor del dominio, no solo en REST, y coinciden con las columnas de MySQL.
Se construye con un builder de nombres explícitos para evitar argumentos posicionales
intercambiados.

| Atributo JSON | Campo Java | Columna | Regla |
|---|---|---|---|
| id | id | id VARCHAR(36) | UUID generado por el servidor, máximo 36 |
| marca | brand | brand VARCHAR(100) | obligatorio, máximo 100 |
| imei | imei | imei VARCHAR(15) UNIQUE | texto de 15 dígitos, dígito verificador Luhn |
| pulgadas | screenSize | screen_size DECIMAL(6,2) | mayor que 0, máximo 9999.99, 2 decimales |
| megapixeles | megapixels | megapixels DECIMAL(7,2) | 0 o mayor, máximo 99999.99, 2 decimales |
| ram | ram | ram INT | mayor que 0 |
| almacenamientoPrincipal | primaryStorage | primary_storage INT | mayor que 0 |
| almacenamientoSecundario | secondaryStorage | secondary_storage INT NULL | opcional, 0 o mayor |
| sistemaOperativo | operatingSystem | operating_system VARCHAR(100) | obligatorio, máximo 100 |
| operador | operator | operator_name VARCHAR(100) | obligatorio, máximo 100 |
| tecnologiaBanda | bandTechnology | band_technology VARCHAR(100) | obligatorio, máximo 100 |
| wifi | wifi | wifi BOOLEAN | obligatorio (false es válido) |
| bluetooth | bluetooth | bluetooth BOOLEAN | obligatorio |
| camaras | cameras | cameras INT | mayor que 0 |
| marcaCpu | cpuBrand | cpu_brand VARCHAR(100) | obligatorio, máximo 100 |
| velocidadCpu | cpuSpeed | cpu_speed DECIMAL(6,2) | mayor que 0, máximo 9999.99, 2 decimales |
| nfc | nfc | nfc BOOLEAN | obligatorio |
| huella | fingerprint | fingerprint BOOLEAN | obligatorio |
| ir | infrared | infrared BOOLEAN | obligatorio |
| resisteAgua | waterResistant | water_resistant BOOLEAN | obligatorio |
| cantidadSim | simCount | sim_count INT | mayor que 0 |

El IMEI se mantiene como `String` para conservar ceros iniciales. El algoritmo de Luhn
duplica cada segundo dígito desde la derecha, resta 9 si el resultado supera 9 y exige
que la suma sea múltiplo de 10; así se rechaza, por ejemplo, `012345678901234` y se
acepta `012345678901237`.

## Arquitectura

| Capa | Elementos | Dependencias |
|---|---|---|
| Dominio | CellphoneModel, excepciones de dominio | Ninguna de Spring ni de SQL |
| Aplicación | Commands, query, CellphoneCommandHandler, CellphoneQueryHandler | Puertos de entrada y salida |
| Adaptadores de salida | CellphoneReadRepositoryMySQL, CellphoneWriteRepositoryMySQL | JdbcTemplate con parámetros enlazados |
| Adaptador de entrada | CellphoneRestController, DTO, CellphoneRestMapper, GlobalExceptionHandler | Puertos de entrada |

CQRS separa contratos y responsabilidades sin una segunda base de datos ni mensajería:
`CellphoneCommandUseCase` y `CellphoneWritePort` para escrituras; `CellphoneQueryUseCase`
y `CellphoneReadPort` para lecturas. El handler de consultas usa
`@Transactional(readOnly = true)` y no conoce puertos de escritura. Las escrituras usan
`@Transactional` con el `JdbcTransactionManager` autoconfigurado por Spring Boot.

`UPDATE` y `DELETE` determinan la existencia por el número de filas afectadas
(`WHERE id = ?`), lo que elimina la consulta previa redundante y la condición de carrera
entre comprobar y modificar. MySQL Connector/J informa filas coincidentes, de modo que
actualizar con los mismos valores no produce un 404 falso. Solo la violación de la
restricción `uq_cellphones_imei` se traduce a conflicto de IMEI; otras duplicidades se
informan como conflicto genérico sin exponer SQL.

## Migraciones y datos existentes

Flyway marca en V1 los esquemas preexistentes (`baseline-on-migrate`) y aplica V2 para
crear `cellphones` con IMEI único. V1 usa `CREATE TABLE IF NOT EXISTS` y preserva
`users`; no se eliminan tablas ni columnas. Ningún script inserta usuarios ni credenciales.

## Seguridad y permisos

| Operación | Permiso |
|---|---|
| Registro de usuario (POST /api/users) | Público; el servidor asigna id UUID, rol MEMBER y estado PENDING |
| Login (POST /api/auth/login) | Público; requiere usuario ACTIVE |
| Consultar usuarios y celulares | ADMIN o REVIEWER |
| Crear, actualizar y eliminar celulares | ADMIN |
| Actualizar y eliminar usuarios | ADMIN |

- El registro público ignora `id`, `role` y `status` enviados por el cliente: no es
  posible autoasignarse ADMIN.
- El primer ADMIN se crea solo si no existe ninguno, a partir de `BOOTSTRAP_ADMIN_EMAIL`
  y `BOOTSTRAP_ADMIN_PASSWORD` (mínimo 12 caracteres). Con un ADMIN existente las
  variables se ignoran, por lo que no permiten crear administradores adicionales.
- El correo de bienvenida ya no contiene contraseñas: el usuario define la suya al
  registrarse y se almacena con BCrypt (límite validado de 72 bytes).
- JWT exige `JWT_SECRET` Base64 de al menos 32 bytes, sin valor de respaldo. En cada
  petición se consulta el estado y el rol actuales del usuario: un ADMIN desactivado o
  eliminado pierde el acceso de inmediato aunque su token no haya expirado.

## Contratos REST

| Código | Situación |
|---|---|
| 201 / 200 / 204 | Creación / lectura o actualización / eliminación |
| 400 | JSON mal formado, tipo inválido, validación REST o regla de dominio |
| 401 | Sin autenticación, token inválido o usuario no activo |
| 403 | Rol insuficiente (por ejemplo MEMBER, o REVIEWER al escribir) |
| 404 | Celular o usuario inexistente |
| 409 | IMEI o correo duplicado |
| 500 | Error interno; se registra en el log y la respuesta no revela detalles |

El cuerpo del celular usa los 20 nombres académicos en español más el `id` generado.
Swagger documenta esquemas, validaciones y permisos; la versión publicada se toma del
`pom.xml` mediante `build-info`.

## Verificación

| Suite | Herramienta | Resultado comprobado |
|---|---|---|
| Pruebas unitarias y de API (MockMvc) | Maven Surefire | 297 ejecutadas, 0 fallidas, 0 omitidas |
| Integración MySQL 8.4 + Flyway | Maven Failsafe + Testcontainers | 15 definidas; omitidas en el equipo local por falta de Docker |
| Empaquetado | mvnw.cmd clean verify | BUILD SUCCESS, users-management-2.2.0.jar |

La línea base de la auditoría tenía 213 pruebas. Las pruebas de integración cubren
migraciones en base nueva y en esquema previo, CRUDL completo REST → dominio → SQL →
dominio → REST, IMEI duplicado al crear y actualizar, identificadores inexistentes,
rollback transaccional, permisos con JWT reales, registro con `role=ADMIN` y revocación
de un ADMIN desactivado. En GitHub Actions se ejecutan con `IT_REQUIRE_MYSQL=true`; su
resultado debe confirmarse en la primera ejecución del pipeline tras publicar la rama.

## Ejecución local

Defina `DB_USERNAME`, `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, un `JWT_SECRET` aleatorio y,
para la primera ejecución, `BOOTSTRAP_ADMIN_EMAIL` y `BOOTSTRAP_ADMIN_PASSWORD` en `.env`.
Luego ejecute `docker compose up --build`. MySQL no se publica en el anfitrión y la API
queda en `127.0.0.1:8080`. Postman contiene la colección CRUDL y un entorno sin
credenciales reales.
