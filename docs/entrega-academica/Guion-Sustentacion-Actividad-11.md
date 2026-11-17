# Guion de sustentación — Actividad 11: CRUDL de Celular

Guion para exponer el proyecto, su arquitectura, el CRUDL, las decisiones técnicas, las
pruebas y una demostración en vivo. Los tiempos son una **sugerencia**: el enunciado no
fija una duración.

| Bloque | Tema | Tiempo sugerido |
|---|---|---|
| 1 | Contexto y objetivo | 1 min |
| 2 | Arquitectura Hexagonal y CQRS | 3 min |
| 3 | Dominio Celular y validaciones | 2 min |
| 4 | API REST y seguridad | 2 min |
| 5 | Persistencia y pruebas | 2 min |
| 6 | Demostración | 4 min |
| 7 | Decisiones técnicas y cierre | 1 min |

## 1. Contexto y objetivo

- Partimos de un microservicio de usuarios en Java 17 y Spring Boot 3.3.5 con JDBC, MySQL, JWT y BCrypt.
- El objetivo fue agregar Celular con CRUDL completo aplicando DDD, Arquitectura Hexagonal y CQRS, sin romper el módulo de usuarios.
- Además se corrigieron hallazgos de seguridad del módulo de usuarios detectados en una auditoría.

## 2. Arquitectura Hexagonal y CQRS

**Qué decir:** el dominio no conoce Spring ni SQL; todo entra y sale por puertos.

- **Entrada:** `CellphoneRestController` → puertos `CellphoneCommandUseCase` / `CellphoneQueryUseCase`.
- **Aplicación:** `CellphoneCommandHandler` (escritura, `@Transactional`) y `CellphoneQueryHandler` (lectura, `@Transactional(readOnly = true)`).
- **Salida:** `CellphoneWritePort` y `CellphoneReadPort`, implementados por `CellphoneWriteRepositoryMySQL` y `CellphoneReadRepositoryMySQL`.
- **CQRS:** separación de contratos y transacciones sobre la misma base; no hay segunda base ni mensajería, y conviene decirlo explícitamente.
- **Detalle técnico:** `UPDATE` y `DELETE` detectan un id inexistente por el número de filas afectadas, sin un `SELECT` previo.

**Mostrar en el IDE:** el paquete `domain` (sin imports de Spring) y los dos handlers.

## 3. Dominio Celular y validaciones

- `CellphoneModel` es un `record` inmutable con 21 atributos; las reglas viven en su constructor.
- **IMEI:** `String` de 15 dígitos (conserva ceros iniciales) con dígito verificador **Luhn**. Ejemplo válido: `012345678901237`; inválido: `012345678901234`.
- **Límites alineados con MySQL:** textos de hasta 100 caracteres y decimales `DECIMAL(6,2)` / `DECIMAL(7,2)`.
- Los booleanos son obligatorios y `false` es un valor válido.
- Con 21 atributos es fácil intercambiar parámetros; por eso se usan builders con nombre.

**Mostrar:** `CellphoneModel.java` y `CellphoneModelTest` (pruebas parametrizadas por campo).

## 4. API REST y seguridad

| Operación | Endpoint | Rol |
|---|---|---|
| Crear | `POST /api/cellphones` | ADMIN |
| Listar / consultar | `GET /api/cellphones`, `GET /api/cellphones/{id}` | ADMIN o REVIEWER |
| Actualizar / eliminar | `PUT` / `DELETE /api/cellphones/{id}` | ADMIN |

- Códigos: 201/200/204; 400 datos inválidos; 401 sin token; 403 rol insuficiente; 404 inexistente; 409 IMEI duplicado.
- El registro público asigna siempre MEMBER; enviar `role=ADMIN` no eleva privilegios.
- El primer ADMIN se crea con variables `BOOTSTRAP_ADMIN_*`, sin credenciales en SQL.
- Si un ADMIN es desactivado, su token deja de servir en la siguiente petición.

## 5. Persistencia y pruebas

- `JdbcTemplate` con parámetros enlazados; Flyway V1 (`users`) y V2 (`cellphones` con `uq_cellphones_imei`).
- **Pruebas unitarias y de API:** 297 aprobadas (dominio 162, aplicación 45, adaptadores 36, REST 50, JWT 4).
- **Integración:** 15 pruebas con Testcontainers y MySQL 8.4. En GitHub Actions se ejecutaron: 14 aprobaron y 1 falló por un problema de aislamiento de la propia prueba, ya corregido en el commit `1e9ca7d`. Antes de la sustentación, confirmar el resultado de la nueva ejecución y actualizar esta frase.

## 6. Demostración

**Preparación (antes de la sustentación):**

1. Crear `.env` desde `.env.example` con contraseñas locales, `JWT_SECRET` aleatorio y `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD`.
2. Ejecutar `docker compose up --build` y comprobar `GET http://localhost:8080/health`.
3. Importar en Postman la colección y el entorno de la carpeta `postman/`; completar `adminEmail`, `adminPassword` y `userPassword`.

**Secuencia sugerida (colección Postman o Swagger UI en `/swagger-ui.html`):**

1. **Auth → Login** con el ADMIN inicial: se guarda el token.
2. **Cellphones → Create:** 201 con UUID generado por el servidor.
3. **List** y **Get by ID:** 200; mostrar que el IMEI conserva el cero inicial.
4. **Update:** 200.
5. **Duplicate IMEI:** 409.
6. **Invalid IMEI check digit - Luhn:** 400.
7. **Missing ID:** 404; **Unauthenticated list:** 401.
8. **Users → Escalation attempt role=ADMIN:** se crea, pero como MEMBER.
9. **Cellphones → Delete:** 204.

**Plan B si Docker no está disponible:** mostrar las pruebas MockMvc de `CellphoneRestControllerTest`, la colección Postman y el registro de GitHub Actions.

## 7. Decisiones técnicas

| Decisión | Alternativa descartada | Motivo |
|---|---|---|
| IMEI como `String` + Luhn | Tipo numérico | Conserva ceros iniciales y valida el dígito verificador |
| Validar en el dominio y en REST | Solo Bean Validation | Ningún adaptador puede persistir datos inválidos |
| CQRS sobre la misma base | Base de lectura separada | Separa responsabilidades sin infraestructura adicional |
| Existencia por filas afectadas | `SELECT` previo | Una consulta menos y sin condición de carrera |
| Revalidar el usuario del JWT | Confiar solo en el token | Revoca de inmediato a usuarios desactivados o degradados |
| Testcontainers con MySQL 8.4 | H2 o mocks de JDBC | Prueba el SQL y las migraciones reales |

## 8. Preguntas probables

- **¿Por qué no hay dos bases para CQRS?** No se requiere para este volumen; la separación está en contratos, handlers y transacciones, y podría evolucionar sin cambiar el dominio.
- **¿Qué pasa si dos peticiones crean el mismo IMEI a la vez?** La restricción única de MySQL lo impide y el adaptador lo traduce a 409.
- **¿Cómo se crea un administrador adicional?** Un ADMIN promueve a un usuario registrado con `PUT /api/users/{id}`.
- **¿Qué no se verificó?** El despliegue en Render y, al momento de escribir este guion, la ejecución de las 15 pruebas de integración tras la corrección.

## 9. Cierre

La entidad Celular quedó integrada con un dominio validado, CQRS explícito, seguridad por
roles reforzada y pruebas en tres niveles; la arquitectura permite extenderla sin acoplar
el dominio a la infraestructura.
