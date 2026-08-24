package cl.duoc.tienda.controller;

import cl.duoc.tienda.model.Product;
import cl.duoc.tienda.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/productos")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<Product> list() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Product product) {
        if (product.getNombre() == null || product.getNombre().isBlank()
                || product.getPrecio() == null
                || product.getStock() == null
                || product.getStock() < 0
                || product.getCategoria() == null || product.getCategoria().isBlank()) {
            return ResponseEntity.badRequest().body("Datos de producto inválidos");
        }

        product.setId(null);
        return ResponseEntity.ok(productRepository.save(product));
    }
}
