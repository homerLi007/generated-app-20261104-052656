package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Value("${app.allowed-root:./allowed_root}")
    private String allowedRoot;

    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteFiles(@RequestBody DeleteRequest request) {
        List<String> deleted = new ArrayList<>();
        List<Map<String, String>> rejected = new ArrayList<>();

        Path root;
        try {
            root = Paths.get(allowedRoot).toRealPath();
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "allowed_root not accessible: " + allowedRoot));
        }

        if (request == null || request.paths == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "paths is required"));
        }

        for (String raw : request.paths) {
            if (raw == null || raw.isBlank()) {
                rejected.add(Map.of("path", String.valueOf(raw), "reason", "empty path"));
                continue;
            }
            try {
                Path candidate = root.resolve(raw).normalize();
                if (!candidate.startsWith(root)) {
                    rejected.add(Map.of("path", raw, "reason", "outside allowed_root"));
                    continue;
                }
                if (Files.isSymbolicLink(candidate)) {
                    rejected.add(Map.of("path", raw, "reason", "symlink refused"));
                    continue;
                }
                if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
                    rejected.add(Map.of("path", raw, "reason", "does not exist"));
                    continue;
                }
                Path real = candidate.toRealPath(LinkOption.NOFOLLOW_LINKS);
                if (!real.startsWith(root)) {
                    rejected.add(Map.of("path", raw, "reason", "resolves outside allowed_root"));
                    continue;
                }
                if (!Files.isRegularFile(real, LinkOption.NOFOLLOW_LINKS)) {
                    rejected.add(Map.of("path", raw, "reason", "not a regular file"));
                    continue;
                }
                Files.delete(real);
                deleted.add(raw);
            } catch (IOException | InvalidPathException e) {
                rejected.add(Map.of("path", raw, "reason", e.getClass().getSimpleName() + ": " + e.getMessage()));
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deleted", deleted);
        body.put("rejected", rejected);
        return ResponseEntity.ok(body);
    }

    public static class DeleteRequest {
        public List<String> paths;
    }
}
