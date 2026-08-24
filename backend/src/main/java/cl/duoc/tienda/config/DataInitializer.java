package cl.duoc.tienda.config;

import cl.duoc.tienda.model.AppUser;
import cl.duoc.tienda.model.Product;
import cl.duoc.tienda.model.Role;
import cl.duoc.tienda.repository.ProductRepository;
import cl.duoc.tienda.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(
            UserRepository userRepository,
            ProductRepository productRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            if (userRepository.count() == 0) {
                userRepository.save(new AppUser(
                        "admin",
                        passwordEncoder.encode("admin123"),
                        "admin@tienda.local",
                        Role.ADMIN));

                userRepository.save(new AppUser(
                        "vendedor",
                        passwordEncoder.encode("vendedor123"),
                        "vendedor@tienda.local",
                        Role.VENDEDOR));

                userRepository.save(new AppUser(
                        "cliente",
                        passwordEncoder.encode("cliente123"),
                        "cliente@tienda.local",
                        Role.CLIENTE));
            }

            if (productRepository.count() == 0) {
                productRepository.save(new Product(
                        "Notebook Lenovo",
                        "Notebook de demostración con 16 GB RAM y SSD de 512 GB",
                        new BigDecimal("649990.00"),
                        15,
                        "Computación"));

                productRepository.save(new Product(
                        "Mouse inalámbrico",
                        "Mouse inalámbrico para demostración",
                        new BigDecimal("19990.00"),
                        40,
                        "Accesorios"));
            }
        };
    }
}
