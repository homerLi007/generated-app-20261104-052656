# Safe File Deleter

A small full-stack demo that lets you delete files inside a sandboxed directory (`allowed_root`) via a web UI. All paths are validated server-side to prevent path traversal, symlink escapes, and deletion of anything outside the allowed root.

## Stack

Spring Boot 3.2 / Java 17 / React 18 / Vite 5

## Layout

```
.
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/demo/
│       │   ├── DemoApplication.java
│       │   └── controller/ApiController.java
│       └── resources/application.properties
└── frontend/
    ├── index.html
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.jsx
        ├── App.jsx
        └── App.css
```

## How to run

Backend:

```bash
cd backend
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. Files are resolved relative to `app.allowed-root` (default `./allowed_root`), so create that directory before use:

```bash
mkdir -p backend/allowed_root
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

The dev server runs on `http://localhost:5173` and proxies `/api` requests to the backend.

## API

### `POST /api/delete`

Deletes one or more files under `allowed_root`.

**Request body**

```json
{
  "paths": ["file1.txt", "subdir/file2.txt"]
}
```

**Response `200 OK`**

```json
{
  "deleted": ["file1.txt"],
  "rejected": [
    { "path": "subdir/file2.txt", "reason": "does not exist" }
  ]
}
```

Each path is validated independently. A path is rejected if it is empty, resolves outside `allowed_root`, is a symlink, does not exist, is not a regular file, or fails to resolve. Rejections do not abort the request — the remaining paths are still processed.

**Errors**

- `400 Bad Request` — missing or malformed `paths` field.
- `500 Internal Server Error` — `allowed_root` is not accessible.

## Notes

- `allowed_root` is configured via `app.allowed-root` in `application.properties` and is resolved to its real path at request time.
- Symlinks are refused outright, and every candidate path is re-checked with `toRealPath(NOFOLLOW_LINKS)` to guard against traversal and TOCTOU-style escapes.
- Only regular files are deleted; directories are rejected.
- There is no authentication or authorization — do not expose this service on an untrusted network.
- The frontend is a minimal single-page form; there is no routing, state persistence, or file browser.
- CORS is not configured on the backend; the Vite dev proxy is expected to be used during development. For production, serve the built frontend behind the same origin as the API or add explicit CORS configuration.