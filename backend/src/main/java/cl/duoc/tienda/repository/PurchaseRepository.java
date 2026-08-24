package cl.duoc.tienda.repository;

import cl.duoc.tienda.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findByClienteUsernameOrderByFechaDesc(String username);
}
