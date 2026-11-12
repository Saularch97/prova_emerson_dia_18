package crudmanager.application.service;

import crudmanager.application.port.in.AuthUseCase;
import crudmanager.application.port.out.PasswordHasher;
import crudmanager.application.port.out.UserDAO;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.User;
import java.util.Map;
import java.util.Optional;

public class AuthService implements AuthUseCase {

    private final UserDAO users;
    private final PasswordHasher hasher;

    public AuthService(UserDAO users, PasswordHasher hasher) {
        this.users = users;
        this.hasher = hasher;
    }

    @Override
    public Optional<User> login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            throw new ValidationException(Map.of("login", "Informe e-mail e senha"));
        }
        User user = users.findByEmail(email.trim());
        if (user == null || !hasher.matches(password, user.getPasswordHash())) {
            return Optional.empty();
        }
        return Optional.of(user);
    }
}
