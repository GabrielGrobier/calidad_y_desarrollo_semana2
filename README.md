# Tienda Segura - CDY2203 Semana 2 b4

Proyecto demostrativo con tres contenedores:

- **Apache HTTPD**: termina HTTPS en `443` y funciona como reverse proxy.
- **Spring Boot**: API REST con Spring Security + JWT en `8080` dentro de la red Docker.
- **MySQL**: persistencia de usuarios, productos y compras en `3306` dentro de la red Docker.

## Arquitectura

```text
Cliente HTTPS
    |
    v
Apache :443
    |
    | http://app:8080
    v
Spring Boot
    |
    | jdbc:mysql://mysql:3306/tienda
    v
MySQL
```


## Frontend web incluido

Apache sirve ahora un frontend estático desde `https://localhost/`. El navegador **no accede directamente** al puerto 8080 del backend: todas las llamadas del frontend se realizan a `/api/*` y Apache las reenvía internamente a `http://app:8080/`.

Flujo:

```text
Navegador --HTTPS :443--> Apache
                         |-- /            -> HTML/CSS/JS
                         `-- /api/*       -> Spring Boot app:8080
                                                |
                                                `-> MySQL mysql:3306
```

El frontend permite:

- iniciar sesión y obtener un JWT;
- visualizar el JWT decodificado (Header, Payload y Signature);
- listar productos;
- comprar productos con rol `CLIENTE`;
- consultar compras propias;
- crear productos con rol `ADMIN`;
- consultar todas las compras con rol `ADMIN` o `VENDEDOR`;
- observar en DevTools el header `Authorization: Bearer <token>`.

Las rutas que ve el navegador tienen prefijo `/api`, por ejemplo `POST /api/login`. Apache elimina ese prefijo al reenviar la solicitud, por lo que Spring Boot sigue usando sus rutas originales (`/login`, `/productos`, `/compras`).

## Usuarios de prueba

| Usuario | Clave | Rol |
|---|---|---|
| admin | admin123 | ADMIN |
| vendedor | vendedor123 | VENDEDOR |
| cliente | cliente123 | CLIENTE |

Las contraseñas quedan almacenadas con BCrypt en MySQL al iniciar por primera vez.

## Levantar con Docker Compose

Desde la raíz:

```bash
docker compose up -d --build
docker compose ps
```

Probar:

```bash
curl -k https://localhost/api/health
```

> `-k` se utiliza porque el certificado incluido es autofirmado y fue generado sólo para el laboratorio local.

## Login

```bash
curl -k -X POST 'https://localhost/login' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'user=cliente' \
  --data-urlencode 'encryptedPass=cliente123'
```

Copiar el valor `token` de la respuesta.

## Consultar productos

```bash
curl -k https://localhost/productos \
  -H 'Authorization: Bearer TOKEN_AQUI'
```

## Crear producto como ADMIN

Obtener primero un token usando `admin / admin123`.

```bash
curl -k -X POST https://localhost/productos \
  -H 'Authorization: Bearer TOKEN_ADMIN' \
  -H 'Content-Type: application/json' \
  -d '{
    "nombre":"Monitor 27 pulgadas",
    "descripcion":"Monitor para demostración",
    "precio":189990,
    "stock":20,
    "categoria":"Computación"
  }'
```

## Registrar compra como CLIENTE

```bash
curl -k -X POST https://localhost/compras \
  -H 'Authorization: Bearer TOKEN_CLIENTE' \
  -H 'Content-Type: application/json' \
  -d '{"productoId":1,"cantidad":1}'
```

## Ver mis compras como CLIENTE

```bash
curl -k https://localhost/compras/mis-compras \
  -H 'Authorization: Bearer TOKEN_CLIENTE'
```

## Ver todas las compras como ADMIN o VENDEDOR

```bash
curl -k https://localhost/compras \
  -H 'Authorization: Bearer TOKEN_ADMIN_O_VENDEDOR'
```

## Demostración de autorización

Si `cliente` intenta crear un producto:

```bash
curl -k -i -X POST https://localhost/productos \
  -H 'Authorization: Bearer TOKEN_CLIENTE' \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"No permitido","descripcion":"Prueba","precio":1000,"stock":1,"categoria":"Prueba"}'
```

Debe recibir `403 Forbidden`.

## Levantar manualmente los contenedores

Crear primero la red:

```bash
docker network create tienda-network
```

### 1. MySQL

```bash
docker build -t tienda-mysql:latest ./mysql

docker run -d \
  --name mysql \
  --network tienda-network \
  -e MYSQL_ROOT_PASSWORD=password \
  -e MYSQL_DATABASE=tienda \
  -e MYSQL_USER=myuser \
  -e MYSQL_PASSWORD=password \
  tienda-mysql:latest
```

Esperar a que MySQL termine de iniciar:

```bash
docker logs -f mysql
```

### 2. Backend

```bash
docker build -t tienda-backend:latest ./backend

docker run -d \
  --name app \
  --network tienda-network \
  -e SPRING_DATASOURCE_URL='jdbc:mysql://mysql:3306/tienda?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  -e SPRING_DATASOURCE_USERNAME=myuser \
  -e SPRING_DATASOURCE_PASSWORD=password \
  tienda-backend:latest
```

Para mostrar el backend directamente durante la clase puedes agregar `-p 8080:8080`, aunque en la arquitectura final no es necesario publicarlo.

### 3. Apache

```bash
docker build -t tienda-apache:latest ./apache

docker run -d \
  --name apache \
  --network tienda-network \
  -p 80:80 \
  -p 443:443 \
  tienda-apache:latest
```

Luego:

```text
https://localhost
```

## Rutas

| Método | Ruta | Acceso |
|---|---|---|
| GET | `/` | Pública |
| GET | `/health` | Pública |
| POST | `/login` | Pública |
| GET | `/productos` | JWT, cualquier usuario autenticado |
| GET | `/productos/{id}` | JWT, cualquier usuario autenticado |
| POST | `/productos` | JWT + ADMIN |
| POST | `/compras` | JWT + CLIENTE |
| GET | `/compras/mis-compras` | JWT + CLIENTE |
| GET | `/compras` | JWT + ADMIN o VENDEDOR |

## Detener y limpiar Compose

```bash
docker compose down
```

Para eliminar además los datos de MySQL:

```bash
docker compose down -v
```

## Nota de seguridad

Este es un proyecto de laboratorio. El certificado TLS es autofirmado, las credenciales son de demostración y la clave JWT está configurada con un valor por defecto para facilitar la clase. En un ambiente real estos valores deben administrarse como secretos.
