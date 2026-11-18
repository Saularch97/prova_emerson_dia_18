package crudmanager.service.impl;

import crudmanager.dao.UserDAO;
import crudmanager.exception.ValidationException;
import crudmanager.model.User;
import crudmanager.security.PasswordHasher;
import crudmanager.service.AuthService;
import java.util.Map;
import java.util.Optional;

public class AuthServiceImpl implements AuthService {

    private final UserDAO users;
    private final PasswordHasher hasher;

    public AuthServiceImpl(UserDAO users, PasswordHasher hasher) {
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
