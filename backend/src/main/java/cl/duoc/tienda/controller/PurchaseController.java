package cl.duoc.tienda.controller;

import cl.duoc.tienda.model.AppUser;
import cl.duoc.tienda.model.Product;
import cl.duoc.tienda.model.Purchase;
import cl.duoc.tienda.repository.ProductRepository;
import cl.duoc.tienda.repository.PurchaseRepository;
import cl.duoc.tienda.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/compras")
public class PurchaseController {

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public PurchaseController(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(
            @RequestBody PurchaseRequest request,
            Authentication authentication) {

        if (request.productoId() == null || request.cantidad() == null || request.cantidad() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "productoId y cantidad son obligatorios"));
        }

        Product product = productRepository.findById(request.productoId()).orElse(null);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        if (product.getStock() < request.cantidad()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Stock insuficiente"));
        }

        AppUser cliente = userRepository.findByUsername(authentication.getName())
                .orElseThrow();

        BigDecimal total = product.getPrecio().multiply(BigDecimal.valueOf(request.cantidad()));

        Purchase purchase = new Purchase();
        purchase.setCliente(cliente);
        purchase.setProducto(product);
        purchase.setCantidad(request.cantidad());
        purchase.setPrecioUnitario(product.getPrecio());
        purchase.setTotal(total);
        purchase.setFecha(LocalDateTime.now());

        product.setStock(product.getStock() - request.cantidad());
        productRepository.save(product);

        return ResponseEntity.ok(purchaseRepository.save(purchase));
    }

    @GetMapping
    public List<Purchase> listAll() {
        return purchaseRepository.findAll();
    }

    @GetMapping("/mis-compras")
    public List<Purchase> myPurchases(Authentication authentication) {
        return purchaseRepository.findByClienteUsernameOrderByFechaDesc(authentication.getName());
    }

    public record PurchaseRequest(Long productoId, Integer cantidad) {}
}
