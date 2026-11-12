package crudmanager.application.port.in;

import crudmanager.domain.model.User;
import java.util.Optional;

public interface AuthUseCase {
    /**
     * @return o usuário autenticado, ou vazio se e-mail ou senha não conferem
     * @throws crudmanager.domain.error.ValidationException se e-mail ou senha chegam vazios
     */
    Optional<User> login(String email, String password);
}
