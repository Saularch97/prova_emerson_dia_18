package crudmanager.application.port.in;

import crudmanager.domain.Page;
import crudmanager.domain.model.User;

/** Porta de entrada dos casos de uso de User. */
public interface UserUseCase {
    /** Página solicitada, já normalizada (acima da última vira a última). */
    Page<User> list(int page);

    /** @throws crudmanager.domain.error.NotFoundException se o id não existe */
    User find(int id);

    /** A senha em texto é obrigatória; só o hash é gravado. */
    void create(User user, String password);

    /** Senha em branco mantém a atual. */
    void update(User user, String password);

    /** @throws crudmanager.domain.error.NotFoundException se o id não existe */
    void delete(int id);
}
