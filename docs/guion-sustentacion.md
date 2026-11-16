# Guion de sustentación — Actividad 11

## 1. Contexto

El punto de partida fue un microservicio de usuarios con Java 17, Spring Boot, JDBC,
MySQL, JWT y BCrypt. Se preservaron sus endpoints, autenticación y notificaciones.

## 2. Dominio

Celular tiene los atributos solicitados. El IMEI es una cadena de 15 dígitos para
conservar ceros iniciales y su unicidad se protege también en la base de datos. Los
valores técnicos inválidos se rechazan y los booleanos falsos son valores válidos.

## 3. Hexagonal y CQRS

El controlador delega en puertos de entrada. El handler de comandos depende de puertos
de persistencia, nunca de JDBC. Las consultas tienen su propio handler y puerto de
lectura. Los adaptadores JDBC implementan esos contratos y usan sentencias parametrizadas.

## 4. API y seguridad

Se puede mostrar POST, GET, PUT y DELETE para `/api/cellphones` y comparar el CRUDL de
Usuario. ADMIN realiza cambios; ADMIN y REVIEWER consultan. El registro público y el
login existentes se mantienen. JWT requiere un secreto aleatorio codificado en Base64;
las contraseñas de Usuario siguen usando BCrypt.

## 5. Datos y pruebas

Flyway agrega la tabla mediante una migración hacia adelante, baselina el esquema previo
y evita eliminar datos. Se presentan pruebas de dominio, casos de uso y MockMvc. La
colección Postman muestra los flujos positivos y errores esperados.

## 6. Cierre

Docker Compose permite iniciar MySQL y la API con variables locales; GitHub Actions
compila, prueba y construye la imagen. Render conserva el despliegue vinculado a `main`;
la rama académica no se despliega a producción automáticamente.
