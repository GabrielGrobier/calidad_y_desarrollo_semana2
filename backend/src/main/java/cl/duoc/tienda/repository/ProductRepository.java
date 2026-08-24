package cl.duoc.tienda.repository;

import cl.duoc.tienda.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
