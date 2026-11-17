# 1. Introducción

Este informe describe la Actividad 11: la incorporación de la entidad Celular a un
microservicio existente de gestión de usuarios, con operaciones CRUDL (crear, leer,
actualizar, eliminar y listar) expuestas mediante una API REST. La solución se construyó
sobre Java 17 y Spring Boot, aplicando Diseño Guiado por el Dominio (DDD), Arquitectura
Hexagonal (puertos y adaptadores) y separación de comandos y consultas (CQRS).

## 1.1 Objetivo general

Extender el microservicio con un CRUDL de celulares seguro, validado y persistido en
MySQL, preservando la arquitectura y la funcionalidad previa del módulo de usuarios.

## 1.2 Objetivos específicos

1. Modelar Celular como un objeto de dominio con sus 21 atributos y reglas de validación.
2. Separar el lado de escritura (comandos) del lado de lectura (consultas).
3. Persistir los datos con Spring JDBC y versionar el esquema con Flyway.
4. Proteger las operaciones con JWT y roles: ADMIN y REVIEWER consultan; solo ADMIN escribe.
5. Verificar el comportamiento con pruebas unitarias y pruebas de integración sobre MySQL real.

# 2. Alcance

| Incluido | No incluido |
|---|---|
| CRUDL de celulares en `/api/cellphones` | Interfaz gráfica de usuario |
| Validación de los 21 atributos en el dominio y en REST | Despliegue verificado en Render |
| Persistencia en MySQL con migraciones Flyway | Segunda base de datos o mensajería para CQRS |
| Seguridad por roles con JWT | Recuperación de contraseña por correo |
| Correcciones de seguridad del módulo Usuario | Cambios en la antigua CLI de escritorio (inactiva) |
| Pruebas unitarias, de API y de integración | |

# 3. Tecnologías utilizadas

| Tecnología | Versión | Uso en el proyecto |
|---|---|---|
| Java | 17 | Lenguaje y plataforma de ejecución |
| Spring Boot | 3.3.5 | Contenedor de dependencias, servidor web y configuración |
| Spring Security + JJWT | 6.3.4 / 0.13.0 | Autenticación con JWT y autorización por rol |
| Spring JDBC (`JdbcTemplate`) | Spring Boot 3.3.5 | Acceso a datos con SQL parametrizado |
| MySQL / Connector/J | 8.4 / 9.3.0 | Base de datos relacional |
| Flyway | 10.10.0 | Migraciones versionadas del esquema |
| BCrypt (at.favre) | 0.10.2 | Hash de contraseñas |
| springdoc-openapi | 2.6.0 | Documentación OpenAPI y Swagger UI |
| Lombok | 1.18.36 | Builders y reducción de código repetitivo |
| JUnit 5, Mockito, AssertJ | 5.10.5 / 5.15.2 | Pruebas unitarias y de API |
| Testcontainers | 1.21.4 | MySQL 8.4 real para pruebas de integración |
| Maven / Docker / GitHub Actions | 3.9.6 (Maven) | Construcción, contenedores e integración continua |

# 4. Arquitectura

## 4.1 Arquitectura Hexagonal

El dominio y la aplicación no dependen de Spring Web, de SQL ni de detalles de
infraestructura. La comunicación ocurre a través de puertos (interfaces): los adaptadores
de entrada (REST) invocan puertos de entrada y la aplicación usa puertos de salida que
implementan los adaptadores JDBC.

| Capa | Paquete | Responsabilidad | Elementos de Celular |
|---|---|---|---|
| Dominio | `domain` | Reglas de negocio puras | `CellphoneModel`, excepciones de dominio |
| Aplicación | `application` | Casos de uso y puertos | `CellphoneCommandHandler`, `CellphoneQueryHandler`, comandos y consulta |
| Adaptador de entrada | `infrastructure.entrypoint.rest` | HTTP, DTO y traducción de errores | `CellphoneRestController`, `CellphoneRestMapper`, `GlobalExceptionHandler` |
| Adaptador de salida | `infrastructure.adapter.persistence` | SQL y mapeo de filas | `CellphoneReadRepositoryMySQL`, `CellphoneWriteRepositoryMySQL` |
| Configuración y seguridad | `infrastructure.config`, `infrastructure.security` | Beans, JWT y reglas de acceso | `SecurityConfig`, `JwtAuthenticationFilter` |

## 4.2 CQRS

CQRS se aplica a nivel de contratos y responsabilidades, sin una segunda base de datos:

| Lado | Puerto de entrada | Handler | Puerto de salida | Transacción |
|---|---|---|---|---|
| Escritura | `CellphoneCommandUseCase` | `CellphoneCommandHandler` | `CellphoneWritePort` | `@Transactional` |
| Lectura | `CellphoneQueryUseCase` | `CellphoneQueryHandler` | `CellphoneReadPort` | `@Transactional(readOnly = true)` |

El handler de consultas no conoce el puerto de escritura y el de comandos no consulta
antes de modificar: `UPDATE` y `DELETE` determinan si el registro existe por el número de
filas afectadas, lo que evita una consulta redundante y la condición de carrera entre
comprobar y modificar.

## 4.3 Flujo de una petición de escritura

1. `CellphoneRestController` recibe el JSON y Bean Validation revisa formato y rangos.
2. `CellphoneRestMapper` construye el comando con un builder de campos nombrados.
3. `CellphoneCommandHandler` crea un `CellphoneModel`; el constructor del dominio valida todas las reglas.
4. `CellphoneWriteRepositoryMySQL` ejecuta el SQL parametrizado dentro de la transacción.
5. El resultado vuelve al controlador, que responde con el DTO y el código HTTP correspondiente.

# 5. Implementación de la entidad Celular

## 5.1 Atributos y validaciones

`CellphoneModel` es un `record` inmutable. Sus reglas se validan en el constructor del
dominio, por lo que ningún adaptador puede persistir un celular inválido, y coinciden con
las columnas de la tabla `cellphones`.

| Atributo JSON | Campo Java | Columna MySQL | Regla de validación |
|---|---|---|---|
| id | id | id VARCHAR(36) | UUID generado por el servidor (máximo 36) |
| marca | brand | brand VARCHAR(100) | Obligatorio, máximo 100 caracteres |
| imei | imei | imei VARCHAR(15) UNIQUE | 15 dígitos y dígito verificador Luhn |
| pulgadas | screenSize | screen_size DECIMAL(6,2) | Mayor que 0, máximo 9999.99, dos decimales |
| megapixeles | megapixels | megapixels DECIMAL(7,2) | Cero o mayor, máximo 99999.99, dos decimales |
| ram | ram | ram INT | Mayor que 0 |
| almacenamientoPrincipal | primaryStorage | primary_storage INT | Mayor que 0 |
| almacenamientoSecundario | secondaryStorage | secondary_storage INT NULL | Opcional; si existe, cero o mayor |
| sistemaOperativo | operatingSystem | operating_system VARCHAR(100) | Obligatorio, máximo 100 caracteres |
| operador | operator | operator_name VARCHAR(100) | Obligatorio, máximo 100 caracteres |
| tecnologiaBanda | bandTechnology | band_technology VARCHAR(100) | Obligatorio, máximo 100 caracteres |
| wifi | wifi | wifi BOOLEAN | Obligatorio; `false` es un valor válido |
| bluetooth | bluetooth | bluetooth BOOLEAN | Obligatorio |
| camaras | cameras | cameras INT | Mayor que 0 |
| marcaCpu | cpuBrand | cpu_brand VARCHAR(100) | Obligatorio, máximo 100 caracteres |
| velocidadCpu | cpuSpeed | cpu_speed DECIMAL(6,2) | Mayor que 0, máximo 9999.99, dos decimales |
| nfc | nfc | nfc BOOLEAN | Obligatorio |
| huella | fingerprint | fingerprint BOOLEAN | Obligatorio |
| ir | infrared | infrared BOOLEAN | Obligatorio |
| resisteAgua | waterResistant | water_resistant BOOLEAN | Obligatorio |
| cantidadSim | simCount | sim_count INT | Mayor que 0 |

## 5.2 IMEI y algoritmo de Luhn

El IMEI se conserva como `String` para no perder ceros iniciales. Además de exigir 15
dígitos, el dominio valida el dígito verificador con el algoritmo de Luhn: desde la
derecha se duplica cada segundo dígito, se resta 9 cuando el resultado supera 9 y la suma
total debe ser múltiplo de 10. Por ejemplo, `012345678901237` es válido y
`012345678901234` se rechaza.

## 5.3 Prevención de errores por parámetros posicionales

Celular tiene 21 atributos. Para evitar intercambiar parámetros del mismo tipo, el modelo,
los comandos y la respuesta REST disponen de builders de Lombok, y los mappers asignan cada
valor por nombre (por ejemplo, `.brand(...)` o `.imei(...)`).

# 6. API REST

## 6.1 Endpoints

| Método | Ruta | Descripción | Rol requerido | Respuesta exitosa |
|---|---|---|---|---|
| POST | `/api/cellphones` | Crear celular | ADMIN | 201 Created |
| GET | `/api/cellphones` | Listar celulares | ADMIN o REVIEWER | 200 OK |
| GET | `/api/cellphones/{id}` | Consultar por id | ADMIN o REVIEWER | 200 OK |
| PUT | `/api/cellphones/{id}` | Actualizar celular | ADMIN | 200 OK |
| DELETE | `/api/cellphones/{id}` | Eliminar celular | ADMIN | 204 No Content |

## 6.2 Códigos de error

| Código | Situación |
|---|---|
| 400 | JSON mal formado, tipo de dato inválido, validación REST o regla de dominio (por ejemplo, Luhn) |
| 401 | Petición sin token, token inválido o usuario que ya no está activo |
| 403 | Rol insuficiente: MEMBER en cualquier operación o REVIEWER al escribir |
| 404 | Celular inexistente |
| 409 | IMEI duplicado al crear o al actualizar |
| 500 | Error interno; se registra en el log y la respuesta no expone detalles técnicos |

La API se documenta con OpenAPI en `/v3/api-docs` y Swagger UI en `/swagger-ui.html`; la
versión publicada se toma del `pom.xml`.

# 7. Seguridad

## 7.1 Roles y permisos

| Operación | Permiso |
|---|---|
| Registro de usuario (`POST /api/users`) | Público; el servidor asigna UUID, rol MEMBER y estado PENDING |
| Inicio de sesión (`POST /api/auth/login`) | Público; requiere usuario ACTIVE |
| Consultar usuarios y celulares | ADMIN o REVIEWER |
| Crear, actualizar y eliminar celulares | ADMIN |
| Actualizar y eliminar usuarios | ADMIN |

## 7.2 Medidas implementadas

- **Registro sin escalada de privilegios:** el cuerpo del registro no acepta `id` ni `role`; si el cliente envía `role=ADMIN`, se ignora.
- **Primer administrador:** se crea al arrancar solo si no existe ningún ADMIN, a partir de las variables `BOOTSTRAP_ADMIN_EMAIL` y `BOOTSTRAP_ADMIN_PASSWORD` (mínimo 12 caracteres). Ningún script SQL contiene credenciales.
- **Contraseñas:** se almacenan con BCrypt; el correo de bienvenida no incluye contraseñas y se rechazan entradas de más de 72 bytes, el límite de BCrypt.
- **JWT:** `JWT_SECRET` es obligatorio (Base64, al menos 32 bytes). En cada petición se consultan el estado y el rol actuales del usuario, de modo que un ADMIN desactivado o degradado pierde el acceso de inmediato.

# 8. Persistencia y migraciones

Los repositorios usan `JdbcTemplate` con parámetros enlazados; ningún dato de entrada se
concatena en el SQL. El esquema se versiona con Flyway:

| Migración | Contenido | Comportamiento |
|---|---|---|
| V1 | Tabla `users` | `CREATE TABLE IF NOT EXISTS`; en una base existente sin Flyway se registra como línea base |
| V2 | Tabla `cellphones` | 21 columnas y restricción única `uq_cellphones_imei` |

Las migraciones no eliminan tablas ni columnas. Solo la violación de `uq_cellphones_imei`
se informa como conflicto de IMEI; cualquier otra duplicidad se responde con un conflicto
genérico sin exponer SQL.

# 9. Pruebas

## 9.1 Estrategia

| Tipo | Herramienta | Qué verifica |
|---|---|---|
| Unitarias | JUnit 5, Mockito, AssertJ (Surefire) | Dominio campo por campo, casos de uso, mappers, JWT |
| API | MockMvc con Spring Security | Rutas, roles, validaciones y códigos 400/401/403/404/409 |
| Integración | Testcontainers + MySQL 8.4 + Flyway (Failsafe) | SQL real, migraciones, transacciones y flujo completo sin mocks de JDBC |

## 9.2 Pruebas unitarias y de API

Son las pruebas unitarias (Surefire), que incluyen las de API con MockMvc. Resultado de
`mvnw.cmd clean verify` en el equipo de desarrollo, confirmado por GitHub Actions en la
ejecución 43998749837 (commit `8a12d5d`): 297 pruebas ejecutadas, 0 fallidas y 0 omitidas.

| Área | Pruebas |
|---|---|
| Dominio (modelos, objetos de valor, enumeraciones, eventos, excepciones) | 162 |
| Aplicación (casos de uso, handlers CQRS, mappers) | 45 |
| Adaptadores de persistencia y correo | 36 |
| Entrada REST y escritorio (MockMvc y controladores) | 50 |
| Seguridad (servicio JWT) | 4 |
| Total | 297 |

## 9.3 Pruebas de integración con MySQL

Se definieron 15 pruebas de integración (12 en `CellphoneMySqlIT` y 3 en
`FlywayMigrationIT`), ejecutadas con Failsafe contra MySQL 8.4 mediante Testcontainers y con
Flyway habilitado. Cubren migraciones en base nueva y en esquema previo, el CRUDL
completo REST → dominio → SQL → dominio → REST, IMEI duplicado al crear y actualizar,
identificadores inexistentes, rollback transaccional, permisos con JWT reales, registro con
`role=ADMIN` y la revocación de un ADMIN desactivado.

| Clase | Pruebas | Resultado en GitHub Actions |
|---|---|---|
| `CellphoneMySqlIT` | 12 | 12 aprobadas |
| `FlywayMigrationIT` | 3 | 3 aprobadas |
| Total | 15 | 15 aprobadas, 0 fallidas, 0 omitidas |

En el equipo de desarrollo no había Docker, por lo que allí se informaron como omitidas; en
GitHub Actions `IT_REQUIRE_MYSQL=true` impide omitirlas. En una ejecución anterior
(43994351552, commit `253cee8`) 14 aprobaron y 1 falló por un problema de la propia prueba:
esperaba un único ADMIN en una base compartida en la que otra prueba ya había insertado
otro. Tras la corrección del commit `1e9ca7d`, la ejecución 43998749837 (commit `8a12d5d`)
aprobó las 15.

Evidencia: [pipeline de GitHub Actions, ejecución 43998749837](https://github.com/A-F-G-A/users-management-microservice/actions/runs/43998749837).

# 10. Resultados

| Aspecto | Resultado verificado |
|---|---|
| CRUDL de celulares | Implementado con los cinco endpoints y control por rol |
| Validaciones | 21 atributos validados en el dominio, incluido Luhn para el IMEI |
| Pruebas unitarias y MockMvc | 297 aprobadas, 0 fallidas, 0 omitidas |
| Pruebas de integración | 15 aprobadas (12 `CellphoneMySqlIT`, 3 `FlywayMigrationIT`), ninguna omitida |
| MySQL 8.4 con Testcontainers | Correcto en GitHub Actions |
| Migraciones Flyway | Correctas |
| Docker | `docker compose config` y construcción de la imagen correctas |
| Empaquetado | `users-management-2.2.0.jar` generado |
| Pipeline de GitHub Actions | SUCCESS, ejecución 43998749837 (commit `8a12d5d`) |
| Despliegue en Render | No verificado en esta actividad |

<!-- salto-de-pagina -->

# 11. Limitaciones y trabajo pendiente

- Las pruebas de integración requieren Docker o un MySQL desechable; en el equipo de desarrollo, sin Docker, se omiten y su verificación depende de GitHub Actions.
- El despliegue en Render no se verificó; `render.yaml` despliega solo la rama `main`.
- Si una base existente conserva el antiguo usuario `admin@example.com`, cuyo hash estuvo publicado en el repositorio, debe cambiarse su contraseña o desactivarse.
- No se implementó un flujo de recuperación de contraseña por correo.
- La revalidación del usuario en cada petición añade una consulta por clave primaria.

# 12. Conclusiones

- La Arquitectura Hexagonal permitió agregar Celular sin acoplar el dominio a Spring ni a SQL: el mismo modelo se valida igual sin importar el adaptador que lo invoque.
- CQRS, aplicado como separación de contratos y transacciones, aclaró las responsabilidades de lectura y escritura sin agregar infraestructura adicional.
- Validar en el dominio, y no solo en REST, junto con el algoritmo de Luhn y los builders con nombre, reduce los datos inválidos y los errores de programación.
- Ejecutar las pruebas de integración contra MySQL real en CI validó el SQL, las migraciones y las transacciones; la única falla mostró la importancia de aislar los datos entre pruebas que comparten una base.
- Las correcciones de seguridad (registro sin escalada de privilegios, administrador inicial sin credenciales en SQL y revocación inmediata de tokens) fortalecen el módulo de usuarios existente.
