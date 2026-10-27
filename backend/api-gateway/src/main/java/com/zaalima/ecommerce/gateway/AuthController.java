package com.zaalima.ecommerce.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    public record LoginRequest(String username, String password) {}

    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {}

    private final UserStore users;
    private final JwtEncoder encoder;
    private final Duration ttl;

    public AuthController(UserStore users, JwtEncoder encoder,
                          @Value("${app.jwt.ttl-minutes:60}") long ttlMinutes) {
        this.users = users;
        this.encoder = encoder;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.username() == null || request.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username and password are required"));
        }
        return users.authenticate(request.username(), request.password())
                .<ResponseEntity<?>>map(user -> {
                    Instant now = Instant.now();
                    JwtClaimsSet claims = JwtClaimsSet.builder()
                            .issuer("ecommerce-gateway")
                            .subject(user.username())
                            .issuedAt(now)
                            .expiresAt(now.plus(ttl))
                            .claim("roles", user.roles())
                            .build();
                    String token = encoder.encode(JwtEncoderParameters.from(
                            JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
                    return ResponseEntity.ok(new TokenResponse(token, "Bearer", ttl.toSeconds()));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "invalid credentials")));
    }
}
