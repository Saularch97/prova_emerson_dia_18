package crudmanager.service;

import crudmanager.model.User;
import java.util.Optional;

public interface AuthService {
    /**
     * @return o usuário autenticado, ou vazio se e-mail ou senha não conferem
     * @throws crudmanager.exception.ValidationException se e-mail ou senha chegam vazios
     */
    Optional<User> login(String email, String password);
}
