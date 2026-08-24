package cl.duoc.tienda.controller;

import cl.duoc.tienda.security.JwtService;
import cl.duoc.tienda.service.MyUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class LoginController {

    private final MyUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginController(
            MyUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestParam("user") String username,
            @RequestParam("encryptedPass") String password) {

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "tipo", "Bearer",
                "usuario", username,
                "roles", userDetails.getAuthorities()));
    }
}
