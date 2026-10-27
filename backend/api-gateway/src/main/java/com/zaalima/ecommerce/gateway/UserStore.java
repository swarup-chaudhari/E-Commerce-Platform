package com.zaalima.ecommerce.gateway;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Demo in-memory user directory. Replace with your user service / IdP. */
@Component
public class UserStore {

    public record User(String username, String passwordHash, List<String> roles) {}

    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final Map<String, User> users;

    public UserStore() {
        users = Map.of(
                "alice", new User("alice", encoder.encode("alice123"), List.of("USER")),
                "bob", new User("bob", encoder.encode("bob123"), List.of("USER")),
                "admin", new User("admin", encoder.encode("admin123"), List.of("USER", "ADMIN")));
    }

    public Optional<User> authenticate(String username, String password) {
        return Optional.ofNullable(users.get(username))
                .filter(u -> encoder.matches(password, u.passwordHash()));
    }
}
