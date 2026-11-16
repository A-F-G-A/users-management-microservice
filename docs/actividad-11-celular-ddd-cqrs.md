# Actividad 11: Gestión de celulares

## Objetivo

Extender el microservicio de usuarios existente con operaciones CRUDL para celulares,
conservando Java 17, Spring Boot, MySQL y Spring JDBC. La solución aplica DDD,
Arquitectura Hexagonal, CQRS, SOLID e inversión de dependencias.

## Modelo de dominio

`CellphoneModel` contiene UUID, marca, IMEI, pantalla, megapíxeles, memoria RAM,
almacenamiento, sistema operativo, operador, tecnología de banda, conectividad,
cámaras, CPU, NFC, huella, infrarrojo, resistencia al agua y cantidad de SIM. IMEI es
texto para conservar ceros iniciales. Se valida con 15 dígitos; la tabla impone
unicidad. Las magnitudes positivas no aceptan valores cero o negativos según el campo.
El almacenamiento secundario es opcional. Los booleanos son obligatorios y admiten
`false` sin tratarlo como dato ausente.

## Arquitectura

- **Dominio:** modelo y excepciones sin dependencias de Spring o persistencia.
- **Aplicación:** commands, query y handlers; entrada mediante `CellphoneCommandUseCase`
  y `CellphoneQueryUseCase`; salida por puertos JDBC separados de lectura y escritura.
- **Adaptadores:** `JdbcTemplate` parametrizado para MySQL y mapper JDBC dedicado.
- **REST:** controller fino, DTO validados, mapper HTTP y traducción uniforme de errores.

La lectura no modifica datos. Las escrituras usan `@Transactional` respaldado por
`DataSourceTransactionManager`. CQRS separa responsabilidades y contratos, sin una
segunda base de datos ni mensajería.

## Migraciones y datos existentes

Flyway marca en V1 los esquemas preexistentes y aplica V2 para crear `cellphones` con
IMEI único. V1 usa `CREATE TABLE IF NOT EXISTS` para instalaciones nuevas y preserva
`users`; no se eliminan tablas ni columnas. `schema.sql` permanece para la inicialización
del contenedor MySQL nuevo, mientras Flyway versiona los cambios de la aplicación.

## Seguridad y permisos

El registro público de usuario y el login permanecen disponibles. Lectura de usuarios y
celulares: ADMIN o REVIEWER. Creación, actualización y eliminación: ADMIN. JWT no tiene
secreto de respaldo conocido: la aplicación requiere `JWT_SECRET` Base64 con al menos
32 bytes. Las contraseñas de Usuario continúan protegidas con BCrypt y no se exponen en
respuestas.

## Contratos REST

- `/api/users`: POST, GET; `/api/users/{id}`: GET, PUT, DELETE.
- `/api/cellphones`: POST, GET; `/api/cellphones/{id}`: GET, PUT, DELETE.
- Creación: 201; lectura/actualización: 200; eliminación: 204; validación: 400;
  inexistente: 404; conflicto de IMEI: 409.

El cuerpo del celular usa los 20 nombres académicos en español. Swagger documenta
esquemas, validaciones y permisos.

## Verificación

La línea base ejecutó 194 pruebas sin fallos y construyó el artefacto original.
Durante el desarrollo se amplió la suite con pruebas de dominio, handlers, persistencia JDBC simulada, serialización,
validación y autorización REST. `mvnw.cmd test` valida la suite local; la ejecución de
MySQL real, Docker y Render requiere los servicios y credenciales correspondientes.

## Ejecución local

Defina `DB_USERNAME`, `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD` y un `JWT_SECRET` aleatorio en
`.env`, luego ejecute `docker compose up --build`. Postman contiene la colección CRUDL y
un entorno sin credenciales reales.
