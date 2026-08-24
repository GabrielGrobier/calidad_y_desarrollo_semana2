# Frontend

Este frontend no requiere Node, npm ni un cuarto contenedor. Apache sirve directamente `index.html`, `styles.css` y `app.js`.

Las llamadas JavaScript usan `/api`:

- `POST /api/login` -> Apache -> `POST http://app:8080/login`
- `GET /api/productos` -> Apache -> `GET http://app:8080/productos`
- `POST /api/compras` -> Apache -> `POST http://app:8080/compras`

El JWT se guarda en `sessionStorage` únicamente para fines de demostración y se agrega como:

```http
Authorization: Bearer <JWT>
```

En una aplicación real se deben evaluar cuidadosamente la estrategia de almacenamiento de tokens, XSS, CSRF, CSP, rotación/expiración de tokens y manejo de secretos.
