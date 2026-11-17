# Guion de sustentación — Actividad 11

## 1. Contexto

El punto de partida fue un microservicio de usuarios con Java 17, Spring Boot, JDBC,
MySQL, JWT y BCrypt. Se preservaron sus endpoints, autenticación y notificaciones, y se
corrigieron los hallazgos de la auditoría.

## 2. Dominio

Celular tiene los 21 atributos solicitados. El IMEI es una cadena de 15 dígitos para
conservar ceros iniciales, se valida con el dígito verificador Luhn y su unicidad se
protege también en la base de datos. Longitudes y límites numéricos se validan en el
dominio con los mismos valores que las columnas; los booleanos falsos son válidos.

## 3. Hexagonal y CQRS

El controlador delega en puertos de entrada. El handler de comandos depende solo del
puerto de escritura y nunca de JDBC; el handler de consultas usa el puerto de lectura en
transacciones de solo lectura. Los adaptadores JDBC usan sentencias parametrizadas y los
mappers emplean builders con nombres para no intercambiar parámetros.

## 4. API y seguridad

Se muestra POST, GET, PUT y DELETE para `/api/cellphones`. ADMIN realiza cambios; ADMIN y
REVIEWER consultan; MEMBER recibe 403 y una petición sin token 401. El registro público
siempre crea MEMBER aunque se envíe `role=ADMIN`. El primer ADMIN se crea una sola vez con
variables de entorno, sin credenciales en SQL. El correo de bienvenida no contiene
contraseñas y un ADMIN desactivado pierde el acceso aunque conserve un token.

## 5. Datos y pruebas

Flyway agrega la tabla mediante una migración hacia adelante y baselina el esquema
previo sin eliminar datos. Hay pruebas de dominio campo por campo, casos de uso, MockMvc
con todos los códigos HTTP y pruebas de integración contra MySQL 8.4 real con Testcontainers,
incluidos rollback y migraciones. La colección Postman muestra flujos positivos y errores.

## 6. Cierre

Docker Compose inicia MySQL (sin publicarlo en el anfitrión) y la API con variables
locales; GitHub Actions, con permisos mínimos y acciones fijadas por SHA, ejecuta las
pruebas, valida Compose y construye la imagen. Render conserva el despliegue vinculado a
`main`; la rama académica no se despliega a producción automáticamente.
