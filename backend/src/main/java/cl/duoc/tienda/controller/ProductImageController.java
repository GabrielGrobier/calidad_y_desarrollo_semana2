package cl.duoc.tienda.controller;

import cl.duoc.tienda.model.Product;
import cl.duoc.tienda.repository.ProductRepository;
import cl.duoc.tienda.service.S3StorageService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Exception;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/productos/{id}/imagen")
public class ProductImageController {
    private final ProductRepository products;
    private final S3StorageService storage;

    public ProductImageController(ProductRepository products, S3StorageService storage) {
        this.products = products;
        this.storage = storage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        Product p = products.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        String old = p.getImagenKey();
        String key = storage.uploadProductImage(id, file);
        try {
            p.setImagenKey(key);
            products.save(p);
        } catch (RuntimeException ex) {
            storage.delete(key);
            throw ex;
        }
        if (old != null && !old.isBlank()) {
            try { storage.delete(old); } catch (RuntimeException ignored) { /* cleanup can be retried */ }
        }
        return ResponseEntity.ok(Map.of("productoId", id, "imagenKey", key));
    }

    @GetMapping
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Product p = products.findById(id).orElse(null);
        if (p == null || p.getImagenKey() == null) return ResponseEntity.notFound().build();
        try {
            String key = p.getImagenKey();
            byte[] bytes = storage.download(key);
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(storage.contentType(key)))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=60")
                .body(bytes);
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Product p = products.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        String old = p.getImagenKey();
        if (old == null) return ResponseEntity.noContent().build();
        p.setImagenKey(null);
        products.save(p);
        storage.delete(old);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> badFile(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
